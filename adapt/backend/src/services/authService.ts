import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { query } from '../db/index.js';
import { config } from '../config.js';

interface LoginPayload {
  email: string;
  password: string;
}

interface RegisterPayload {
  email: string;
  password: string;
  first_name?: string;
  last_name?: string;
  phone?: string;
}

interface User {
  id: string;
  email: string;
  role: string;
  first_name: string;
  last_name: string;
}

export const authService = {
  async login(payload: LoginPayload): Promise<{ token: string; user: User }> {
    const { email, password } = payload;

    // Find user by email
    const result = await query(
      'SELECT id, email, password_hash, role, first_name, last_name FROM users WHERE email = $1',
      [email]
    );

    if (result.rows.length === 0) {
      throw { status: 401, message: 'Invalid email or password' };
    }

    const user = result.rows[0];

    // Verify password
    const passwordMatch = await bcrypt.compare(password, user.password_hash);
    if (!passwordMatch) {
      throw { status: 401, message: 'Invalid email or password' };
    }

    // Generate JWT token
    const token = jwt.sign(
      {
        userId: user.id,
        email: user.email,
        role: user.role,
      },
      config.jwt.secret,
      { expiresIn: config.jwt.expiry }
    );

    return {
      token,
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        first_name: user.first_name,
        last_name: user.last_name,
      },
    };
  },

  async createUser(payload: {
    email: string;
    password: string;
    role: string;
    first_name?: string;
    last_name?: string;
    phone?: string;
  }): Promise<User> {
    const { email, password, role, first_name, last_name, phone } = payload;

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    const result = await query(
      `INSERT INTO users (email, password_hash, role, first_name, last_name, phone)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id, email, role, first_name, last_name`,
      [email, passwordHash, role, first_name, last_name, phone]
    );

    return result.rows[0];
  },

  async register(payload: RegisterPayload): Promise<{ token: string; user: User }> {
    const { email, password, first_name, last_name, phone } = payload;

    const existing = await query('SELECT id FROM users WHERE email = $1', [email]);
    if (existing.rows.length > 0) {
      throw { status: 409, message: 'Email is already registered' };
    }

    const createdUser = await this.createUser({
      email,
      password,
      role: 'CAREGIVER',
      first_name,
      last_name,
      phone,
    });

    const token = jwt.sign(
      {
        userId: createdUser.id,
        email: createdUser.email,
        role: createdUser.role,
      },
      config.jwt.secret,
      { expiresIn: config.jwt.expiry }
    );

    return {
      token,
      user: createdUser,
    };
  },

  async getUserById(userId: string): Promise<User | null> {
    const result = await query(
      'SELECT id, email, role, first_name, last_name FROM users WHERE id = $1',
      [userId]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0];
  },

  async verifyToken(token: string): Promise<{ userId: string; email: string; role: string }> {
    try {
      const decoded = jwt.verify(token, config.jwt.secret) as {
        userId: string;
        email: string;
        role: string;
      };
      return decoded;
    } catch (error) {
      throw { status: 401, message: 'Invalid or expired token' };
    }
  },
};
