import { query } from '../db/index.js';

interface CaregiverInput {
  first_name: string;
  last_name: string;
  email?: string;
  phone?: string;
}

interface Caregiver extends CaregiverInput {
  id: string;
  user_id?: string;
  created_at: string;
  updated_at: string;
}

interface CaregiverUserProfile {
  id: string;
  email: string;
  first_name?: string;
  last_name?: string;
  phone?: string;
}

export const caregiverService = {
  async getAll(limit: number = 50, offset: number = 0): Promise<Caregiver[]> {
    const result = await query(
      `SELECT * FROM caregivers 
       ORDER BY created_at DESC 
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return result.rows;
  },

  async getById(id: string): Promise<Caregiver> {
    const result = await query('SELECT * FROM caregivers WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      throw { status: 404, message: 'Caregiver not found' };
    }
    return result.rows[0];
  },

  async create(payload: CaregiverInput): Promise<Caregiver> {
    const { first_name, last_name, email, phone } = payload;

    const result = await query(
      `INSERT INTO caregivers (first_name, last_name, email, phone)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [first_name, last_name, email, phone]
    );

    return result.rows[0];
  },

  async update(id: string, payload: Partial<CaregiverInput>): Promise<Caregiver> {
    const { first_name, last_name, email, phone } = payload;

    const result = await query(
      `UPDATE caregivers 
       SET first_name = COALESCE($1, first_name),
           last_name = COALESCE($2, last_name),
           email = COALESCE($3, email),
           phone = COALESCE($4, phone),
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $5
       RETURNING *`,
      [first_name, last_name, email, phone, id]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Caregiver not found' };
    }

    return result.rows[0];
  },

  async delete(id: string): Promise<void> {
    const result = await query('DELETE FROM caregivers WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw { status: 404, message: 'Caregiver not found' };
    }
  },

  async getCount(): Promise<number> {
    const result = await query('SELECT COUNT(*) as count FROM caregivers');
    return parseInt(result.rows[0].count, 10);
  },

  async getByPatientId(patientId: string): Promise<Caregiver[]> {
    const result = await query(
      `SELECT c.* FROM caregivers c
       INNER JOIN patient_caregiver_links pcl ON c.id = pcl.caregiver_id
       WHERE pcl.patient_id = $1`,
      [patientId]
    );
    return result.rows;
  },

  async getByUserId(userId: string): Promise<Caregiver | null> {
    const result = await query('SELECT * FROM caregivers WHERE user_id = $1 LIMIT 1', [userId]);
    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0];
  },

  async ensureForUser(user: CaregiverUserProfile): Promise<Caregiver> {
    const existing = await this.getByUserId(user.id);
    if (existing) {
      return existing;
    }

    const firstName = user.first_name?.trim() || 'Care';
    const lastName = user.last_name?.trim() || 'Giver';

    const result = await query(
      `INSERT INTO caregivers (user_id, first_name, last_name, email, phone)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [user.id, firstName, lastName, user.email, user.phone || null]
    );

    return result.rows[0];
  },

  async assignToPatient(patientId: string, caregiverId: string, relationship: string = 'PROFESSIONAL'): Promise<void> {
    await query(
      `INSERT INTO patient_caregiver_links (patient_id, caregiver_id, relationship)
       VALUES ($1, $2, $3)
       ON CONFLICT (patient_id, caregiver_id) DO NOTHING`,
      [patientId, caregiverId, relationship]
    );
  },

  async removeFromPatient(patientId: string, caregiverId: string): Promise<void> {
    await query(
      `DELETE FROM patient_caregiver_links 
       WHERE patient_id = $1 AND caregiver_id = $2`,
      [patientId, caregiverId]
    );
  },
};
