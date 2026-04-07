import pg from 'pg';
import { config } from '../config.js';

const { Pool } = pg;

const pool = config.db.url
  ? new Pool({
      connectionString: config.db.url,
      ssl: config.db.ssl ? { rejectUnauthorized: false } : undefined,
    })
  : new Pool({
      host: config.db.host,
      port: config.db.port,
      database: config.db.database,
      user: config.db.user,
      password: config.db.password,
      ssl: config.db.ssl ? { rejectUnauthorized: false } : undefined,
    });

pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
});

export const query = (text: string, params?: any[]) => {
  return pool.query(text, params);
};

export const getClient = async () => {
  return pool.connect();
};

export default pool;
