import { query } from '../db/index.js';

interface DeviceInput {
  patient_id: string;
  device_name: string;
  device_type: string;
  os?: string;
  capabilities?: string[];
}

interface Device extends DeviceInput {
  id: string;
  is_online: boolean;
  last_seen_at?: string;
  created_at: string;
  updated_at: string;
}

export const deviceService = {
  async getAll(limit: number = 50, offset: number = 0): Promise<Device[]> {
    const result = await query(
      `SELECT * FROM devices 
       ORDER BY created_at DESC 
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );
    return result.rows;
  },

  async getById(id: string): Promise<Device> {
    const result = await query('SELECT * FROM devices WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      throw { status: 404, message: 'Device not found' };
    }
    return result.rows[0];
  },

  async getByPatientId(patientId: string): Promise<Device[]> {
    const result = await query(
      `SELECT * FROM devices 
       WHERE patient_id = $1 
       ORDER BY created_at DESC`,
      [patientId]
    );
    return result.rows;
  },

  async create(payload: DeviceInput): Promise<Device> {
    const { patient_id, device_name, device_type, os, capabilities } = payload;

    const result = await query(
      `INSERT INTO devices (patient_id, device_name, device_type, os, capabilities)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [patient_id, device_name, device_type, os, capabilities || []]
    );

    return result.rows[0];
  },

  async update(id: string, payload: Partial<DeviceInput>): Promise<Device> {
    const { device_name, device_type, os, capabilities } = payload;

    const result = await query(
      `UPDATE devices 
       SET device_name = COALESCE($1, device_name),
           device_type = COALESCE($2, device_type),
           os = COALESCE($3, os),
           capabilities = COALESCE($4, capabilities),
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $5
       RETURNING *`,
      [device_name, device_type, os, capabilities, id]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Device not found' };
    }

    return result.rows[0];
  },

  async updateStatus(id: string, isOnline: boolean): Promise<Device> {
    const result = await query(
      `UPDATE devices 
       SET is_online = $1, 
           last_seen_at = CURRENT_TIMESTAMP,
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING *`,
      [isOnline, id]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Device not found' };
    }

    return result.rows[0];
  },

  async delete(id: string): Promise<void> {
    const result = await query('DELETE FROM devices WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw { status: 404, message: 'Device not found' };
    }
  },

  async getCount(): Promise<number> {
    const result = await query('SELECT COUNT(*) as count FROM devices');
    return parseInt(result.rows[0].count, 10);
  },

  async getOnlineCount(): Promise<number> {
    const result = await query('SELECT COUNT(*) as count FROM devices WHERE is_online = true');
    return parseInt(result.rows[0].count, 10);
  },
};
