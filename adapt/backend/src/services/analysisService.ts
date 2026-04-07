import { query } from '../db/index.js';

type GenericRecord = Record<string, any>;

const clampWindow = (value: unknown, fallback: number): number => {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.max(60_000, Math.min(parsed, 30 * 24 * 60 * 60 * 1000));
};

const pickTrend = (params: {
  telemetryTotal: number;
  unacknowledgedAlerts: number;
  criticalAlerts: number;
  severeAlerts: number;
}): string => {
  if (params.telemetryTotal === 0) {
    return 'NO_DATA';
  }

  if (params.criticalAlerts > 0 || params.severeAlerts > 2) {
    return 'DETERIORATING';
  }

  if (params.unacknowledgedAlerts > 0) {
    return 'WATCH';
  }

  return 'STABLE';
};

const recommendationFromTrend = (trend: string): string => {
  if (trend === 'DETERIORATING') {
    return 'Escalate caregiver review immediately and enable high-frequency monitoring.';
  }

  if (trend === 'WATCH') {
    return 'Schedule follow-up checks and clear unacknowledged alerts.';
  }

  if (trend === 'NO_DATA') {
    return 'Collect at least one telemetry cycle to enable analysis.';
  }

  return 'Continue current care plan and review routine completion daily.';
};

export const analysisService = {
  normalizeWindowMs(value: unknown, fallback: number = 86_400_000): number {
    return clampWindow(value, fallback);
  },

  async getOverview(windowMs: number): Promise<GenericRecord> {
    const normalizedWindow = this.normalizeWindowMs(windowMs);
    const now = Date.now();
    const startMs = now - normalizedWindow;
    const startDate = new Date(startMs);

    const [
      patientsResult,
      alertsResult,
      telemetryResult,
      taskPlansResult,
    ] = await Promise.all([
      query('SELECT COUNT(*) AS count FROM patients'),
      query(
        `SELECT
           COUNT(*) AS total,
           COUNT(*) FILTER (WHERE is_acknowledged = false) AS unacknowledged
         FROM alerts
         WHERE created_at >= $1`,
        [startDate]
      ),
      query(
        `SELECT COUNT(*) AS count
         FROM telemetry
         WHERE timestamp_ms >= $1`,
        [startMs]
      ),
      query(
        `SELECT status, COUNT(*) AS count
         FROM task_plans
         WHERE created_at >= $1
         GROUP BY status`,
        [startDate]
      ),
    ]);

    const taskPlansByStatus = taskPlansResult.rows.reduce((acc: Record<string, number>, row: GenericRecord) => {
      acc[String(row.status || 'UNKNOWN')] = parseInt(String(row.count || '0'), 10);
      return acc;
    }, {});

    return {
      window: {
        fromMs: startMs,
        toMs: now,
      },
      totals: {
        patients: parseInt(String(patientsResult.rows[0]?.count || '0'), 10),
        alerts: parseInt(String(alertsResult.rows[0]?.total || '0'), 10),
        unacknowledgedAlerts: parseInt(String(alertsResult.rows[0]?.unacknowledged || '0'), 10),
        telemetrySignals: parseInt(String(telemetryResult.rows[0]?.count || '0'), 10),
      },
      taskLab: {
        byStatus: taskPlansByStatus,
      },
    };
  },

  async getPatientSummary(patientId: string, windowMs: number): Promise<GenericRecord> {
    const normalizedWindow = this.normalizeWindowMs(windowMs);
    const now = Date.now();
    const startMs = now - normalizedWindow;
    const startDate = new Date(startMs);

    const patientResult = await query(
      `SELECT id, first_name, last_name, risk_level
       FROM patients
       WHERE id = $1
       LIMIT 1`,
      [patientId]
    );

    if (patientResult.rows.length === 0) {
      throw { status: 404, message: 'Patient not found' };
    }

    const [
      telemetryTotalResult,
      telemetryByTypeResult,
      alertsTotalsResult,
      alertsBySeverityResult,
      dominantIssueResult,
      taskPlanStatusResult,
    ] = await Promise.all([
      query(
        `SELECT COUNT(*) AS count
         FROM telemetry
         WHERE patient_id = $1 AND timestamp_ms >= $2`,
        [patientId, startMs]
      ),
      query(
        `SELECT signal_type, COUNT(*) AS count
         FROM telemetry
         WHERE patient_id = $1 AND timestamp_ms >= $2
         GROUP BY signal_type
         ORDER BY COUNT(*) DESC`,
        [patientId, startMs]
      ),
      query(
        `SELECT
           COUNT(*) AS total,
           COUNT(*) FILTER (WHERE is_acknowledged = false) AS unacknowledged
         FROM alerts
         WHERE patient_id = $1 AND created_at >= $2`,
        [patientId, startDate]
      ),
      query(
        `SELECT severity, COUNT(*) AS count
         FROM alerts
         WHERE patient_id = $1 AND created_at >= $2
         GROUP BY severity
         ORDER BY COUNT(*) DESC`,
        [patientId, startDate]
      ),
      query(
        `SELECT primary_issue, COUNT(*) AS count
         FROM alerts
         WHERE patient_id = $1
           AND primary_issue IS NOT NULL
           AND primary_issue <> ''
           AND created_at >= $2
         GROUP BY primary_issue
         ORDER BY COUNT(*) DESC
         LIMIT 1`,
        [patientId, startDate]
      ),
      query(
        `SELECT status, COUNT(*) AS count
         FROM task_plans
         WHERE patient_id = $1 AND created_at >= $2
         GROUP BY status`,
        [patientId, startDate]
      ),
    ]);

    const telemetryTotal = parseInt(String(telemetryTotalResult.rows[0]?.count || '0'), 10);
    const unacknowledgedAlerts = parseInt(String(alertsTotalsResult.rows[0]?.unacknowledged || '0'), 10);

    const alertsBySeverity = alertsBySeverityResult.rows.reduce((acc: Record<string, number>, row: GenericRecord) => {
      const severity = String(row.severity || 'UNKNOWN');
      acc[severity] = parseInt(String(row.count || '0'), 10);
      return acc;
    }, {});

    const trend = pickTrend({
      telemetryTotal,
      unacknowledgedAlerts,
      criticalAlerts: alertsBySeverity.CRITICAL || 0,
      severeAlerts: alertsBySeverity.SEVERE || 0,
    });

    const taskPlanByStatus = taskPlanStatusResult.rows.reduce((acc: Record<string, number>, row: GenericRecord) => {
      acc[String(row.status || 'UNKNOWN')] = parseInt(String(row.count || '0'), 10);
      return acc;
    }, {});

    const patientRow = patientResult.rows[0];

    return {
      patient: {
        id: patientRow.id,
        name: `${patientRow.first_name || ''} ${patientRow.last_name || ''}`.trim(),
        riskLevel: String(patientRow.risk_level || 'MEDIUM').toUpperCase(),
      },
      window: {
        fromMs: startMs,
        toMs: now,
      },
      telemetry: {
        total: telemetryTotal,
        byType: telemetryByTypeResult.rows.map((row: GenericRecord) => ({
          signalType: String(row.signal_type || 'UNKNOWN'),
          count: parseInt(String(row.count || '0'), 10),
        })),
      },
      alerts: {
        total: parseInt(String(alertsTotalsResult.rows[0]?.total || '0'), 10),
        unacknowledged: unacknowledgedAlerts,
        bySeverity: alertsBySeverity,
      },
      taskLab: {
        byStatus: taskPlanByStatus,
      },
      insight: {
        dominantIssue: dominantIssueResult.rows[0]?.primary_issue || 'NONE',
        trend,
        recommendation: recommendationFromTrend(trend),
      },
    };
  },
};
