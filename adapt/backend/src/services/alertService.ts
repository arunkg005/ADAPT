import { query } from '../db/index.js';

interface AlertInput {
  patient_id: string;
  task_name?: string;
  task_state?: string;
  severity: string;
  primary_issue?: string;
  assistance_mode?: string;
  engine_output?: any;
}

interface Alert extends AlertInput {
  id: string;
  is_acknowledged: boolean;
  acknowledged_by?: string;
  acknowledged_at?: string;
  created_at: string;
}

export const alertService = {
  async getAll(limit: number = 50, offset: number = 0): Promise<Alert[]> {
    const result = await query(
      `SELECT * FROM alerts 
       ORDER BY created_at DESC 
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return result.rows;
  },

  async getById(id: string): Promise<Alert> {
    const result = await query('SELECT * FROM alerts WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      throw { status: 404, message: 'Alert not found' };
    }
    return result.rows[0];
  },

  async getByPatientId(patientId: string, limit: number = 50, offset: number = 0): Promise<Alert[]> {
    const result = await query(
      `SELECT * FROM alerts 
       WHERE patient_id = $1 
       ORDER BY created_at DESC 
       LIMIT $2 OFFSET $3`,
      [patientId, limit, offset]
    );
    return result.rows;
  },

  async create(payload: AlertInput): Promise<Alert> {
    const { patient_id, task_name, task_state, severity, primary_issue, assistance_mode, engine_output } = payload;

    const result = await query(
      `INSERT INTO alerts (patient_id, task_name, task_state, severity, primary_issue, assistance_mode, engine_output)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [patient_id, task_name, task_state, severity, primary_issue, assistance_mode, JSON.stringify(engine_output)]
    );

    return result.rows[0];
  },

  async acknowledge(id: string, caregiverId: string): Promise<Alert> {
    const result = await query(
      `UPDATE alerts 
       SET is_acknowledged = true, 
           acknowledged_by = $1, 
           acknowledged_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING *`,
      [caregiverId, id]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Alert not found' };
    }

    return result.rows[0];
  },

  async getUnacknowledgedCount(): Promise<number> {
    const result = await query('SELECT COUNT(*) as count FROM alerts WHERE is_acknowledged = false');
    return parseInt(result.rows[0].count, 10);
  },

  async getCountBySeverity(): Promise<any[]> {
    const result = await query(
      `SELECT severity, COUNT(*) as count FROM alerts 
       WHERE is_acknowledged = false 
       GROUP BY severity 
       ORDER BY 
         CASE severity 
           WHEN 'CRITICAL' THEN 1
           WHEN 'SEVERE' THEN 2
           WHEN 'MODERATE' THEN 3
           WHEN 'MILD' THEN 4
           ELSE 5
         END`
    );
    return result.rows;
  },

  async delete(id: string): Promise<void> {
    const result = await query('DELETE FROM alerts WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw { status: 404, message: 'Alert not found' };
    }
  },
};
