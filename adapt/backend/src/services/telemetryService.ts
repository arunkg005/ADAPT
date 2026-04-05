import { query } from '../db/index.js';

interface TelemetryInput {
  patient_id: string;
  device_id?: string;
  signal_type: string;
  signal_value: any;
  timestamp_ms: number;
}

interface Telemetry extends TelemetryInput {
  id: string;
  created_at: string;
}

export const telemetryService = {
  async create(payload: TelemetryInput): Promise<Telemetry> {
    const { patient_id, device_id, signal_type, signal_value, timestamp_ms } = payload;

    const result = await query(
      `INSERT INTO telemetry (patient_id, device_id, signal_type, signal_value, timestamp_ms)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [patient_id, device_id, signal_type, JSON.stringify(signal_value), timestamp_ms]
    );

    return result.rows[0];
  },

  async getByPatientId(
    patientId: string,
    signalType?: string,
    limitMs: number = 3600000 // 1 hour default
  ): Promise<Telemetry[]> {
    const now = Date.now();
    const startTime = now - limitMs;

    const query_text = signalType
      ? `SELECT * FROM telemetry 
         WHERE patient_id = $1 AND signal_type = $2 AND timestamp_ms >= $3 
         ORDER BY timestamp_ms DESC`
      : `SELECT * FROM telemetry 
         WHERE patient_id = $1 AND timestamp_ms >= $2 
         ORDER BY timestamp_ms DESC`;

    const params = signalType ? [patientId, signalType, startTime] : [patientId, startTime];

    const result = await query(query_text, params);
    return result.rows;
  },

  async getByDeviceId(deviceId: string, limitMs: number = 3600000): Promise<Telemetry[]> {
    const now = Date.now();
    const startTime = now - limitMs;

    const result = await query(
      `SELECT * FROM telemetry 
       WHERE device_id = $1 AND timestamp_ms >= $2 
       ORDER BY timestamp_ms DESC`,
      [deviceId, startTime]
    );

    return result.rows;
  },

  async deleteOlderThan(ageMs: number): Promise<number> {
    const cutoffTime = Date.now() - ageMs;
    const result = await query(
      'DELETE FROM telemetry WHERE timestamp_ms < $1',
      [cutoffTime]
    );
    return result.rowCount || 0;
  },

  async getRecentSignalsSummary(patientId: string, limitMs: number = 600000): Promise<any> {
    const now = Date.now();
    const startTime = now - limitMs;

    const result = await query(
      `SELECT signal_type, COUNT(*) as count, MAX(timestamp_ms) as last_occurrence
       FROM telemetry 
       WHERE patient_id = $1 AND timestamp_ms >= $2 
       GROUP BY signal_type 
       ORDER BY last_occurrence DESC`,
      [patientId, startTime]
    );

    return result.rows;
  },
};
