import { query } from '../db/index.js';

interface PatientInput {
  first_name: string;
  last_name: string;
  date_of_birth?: string;
  cognitive_condition?: string;
  risk_level?: string;
  baseline_response_time_ms?: number;
}

interface Patient extends PatientInput {
  id: string;
  user_id?: string;
  created_at: string;
  updated_at: string;
}

export const patientService = {
  async getAll(limit: number = 50, offset: number = 0): Promise<Patient[]> {
    const result = await query(
      `SELECT * FROM patients 
       ORDER BY created_at DESC 
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return result.rows;
  },

  async getById(id: string): Promise<Patient> {
    const result = await query('SELECT * FROM patients WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      throw { status: 404, message: 'Patient not found' };
    }
    return result.rows[0];
  },

  async create(payload: PatientInput): Promise<Patient> {
    const {
      first_name,
      last_name,
      date_of_birth,
      cognitive_condition,
      risk_level = 'MEDIUM',
      baseline_response_time_ms = 3000,
    } = payload;

    const result = await query(
      `INSERT INTO patients (first_name, last_name, date_of_birth, cognitive_condition, risk_level, baseline_response_time_ms)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [first_name, last_name, date_of_birth, cognitive_condition, risk_level, baseline_response_time_ms]
    );

    return result.rows[0];
  },

  async update(id: string, payload: Partial<PatientInput>): Promise<Patient> {
    const { first_name, last_name, cognitive_condition, risk_level, baseline_response_time_ms } = payload;

    const result = await query(
      `UPDATE patients 
       SET first_name = COALESCE($1, first_name),
           last_name = COALESCE($2, last_name),
           cognitive_condition = COALESCE($3, cognitive_condition),
           risk_level = COALESCE($4, risk_level),
           baseline_response_time_ms = COALESCE($5, baseline_response_time_ms),
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $6
       RETURNING *`,
      [first_name, last_name, cognitive_condition, risk_level, baseline_response_time_ms, id]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Patient not found' };
    }

    return result.rows[0];
  },

  async delete(id: string): Promise<void> {
    const result = await query('DELETE FROM patients WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw { status: 404, message: 'Patient not found' };
    }
  },

  async getCount(): Promise<number> {
    const result = await query('SELECT COUNT(*) as count FROM patients');
    return parseInt(result.rows[0].count, 10);
  },
};
