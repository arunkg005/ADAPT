import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import type { Secret, SignOptions } from 'jsonwebtoken';
import { query } from '../db/index.js';
import { config } from '../config.js';

const jwtSecret: Secret = config.jwt.secret;
const jwtSignOptions: SignOptions = {
  expiresIn: config.jwt.expiry as SignOptions['expiresIn'],
};

interface LoginPayload {
  email: string;
  password: string;
  platform?: string;
}

interface RegisterPayload {
  email: string;
  password: string;
  first_name?: string;
  last_name?: string;
  phone?: string;
  role?: string;
  platform?: string;
}

interface SocialLoginPayload {
  provider: string;
  email: string;
  first_name?: string;
  last_name?: string;
  platform?: string;
}

type UserRole = 'ADMIN' | 'CAREGIVER' | 'PATIENT';
type SocialProvider = 'GOOGLE' | 'FACEBOOK';
type ClientPlatform = 'WEB' | 'MOBILE' | 'UNKNOWN';

const VALID_USER_ROLES: UserRole[] = ['ADMIN', 'CAREGIVER', 'PATIENT'];
const PUBLIC_SIGNUP_ROLES: UserRole[] = ['CAREGIVER', 'PATIENT'];
const VALID_SOCIAL_PROVIDERS: SocialProvider[] = ['GOOGLE', 'FACEBOOK'];
const WEB_ALLOWED_ROLES: UserRole[] = ['ADMIN', 'CAREGIVER'];

const normalizeUserRole = (role?: string, defaultRole: UserRole = 'PATIENT'): UserRole => {
  const normalized = (role || defaultRole).trim().toUpperCase();

  if (!VALID_USER_ROLES.includes(normalized as UserRole)) {
    throw { status: 400, message: 'Invalid role. Allowed values: ADMIN, CAREGIVER, PATIENT' };
  }

  return normalized as UserRole;
};

const normalizeSocialProvider = (provider?: string): SocialProvider => {
  const normalized = String(provider || '').trim().toUpperCase();
  if (!VALID_SOCIAL_PROVIDERS.includes(normalized as SocialProvider)) {
    throw { status: 400, message: 'Unsupported social provider. Allowed values: GOOGLE, FACEBOOK' };
  }

  return normalized as SocialProvider;
};

const normalizePlatform = (platform?: string): ClientPlatform => {
  const normalized = String(platform || '').trim().toUpperCase();
  if (normalized === 'WEB') {
    return 'WEB';
  }

  if (normalized === 'MOBILE') {
    return 'MOBILE';
  }

  return 'UNKNOWN';
};

const assertPlatformRoleAccess = (platform: ClientPlatform, role: UserRole): void => {
  if (platform === 'WEB' && !WEB_ALLOWED_ROLES.includes(role)) {
    throw {
      status: 403,
      message: 'Web access is restricted to CAREGIVER and ADMIN accounts',
    };
  }
};

interface User {
  id: string;
  email: string;
  role: UserRole;
  first_name?: string;
  last_name?: string;
  phone?: string;
}

export const authService = {
  async getUserByEmail(email: string): Promise<User | null> {
    const normalizedEmail = String(email || '').trim().toLowerCase();
    const result = await query(
      'SELECT id, email, role, first_name, last_name, phone FROM users WHERE email = $1',
      [normalizedEmail]
    );

    if (result.rows.length === 0) {
      return null;
    }

    const row = result.rows[0];
    return {
      id: row.id,
      email: row.email,
      role: normalizeUserRole(row.role, 'PATIENT'),
      first_name: row.first_name,
      last_name: row.last_name,
      phone: row.phone,
    };
  },

  async login(payload: LoginPayload): Promise<{ token: string; user: User }> {
    const { email, password } = payload;
    const platform = normalizePlatform(payload.platform);
    const normalizedEmail = String(email || '').trim().toLowerCase();

    // Find user by email
    const result = await query(
      'SELECT id, email, password_hash, role, first_name, last_name FROM users WHERE email = $1',
      [normalizedEmail]
    );

    if (result.rows.length === 0) {
      throw { status: 401, message: 'Invalid email or password' };
    }

    const user = result.rows[0];
    const role = normalizeUserRole(user.role, 'PATIENT');

    // Verify password
    const passwordMatch = await bcrypt.compare(password, user.password_hash);
    if (!passwordMatch) {
      throw { status: 401, message: 'Invalid email or password' };
    }

    assertPlatformRoleAccess(platform, role);

    // Generate JWT token
    const token = jwt.sign(
      {
        userId: user.id,
        email: user.email,
        role,
      },
      jwtSecret,
      jwtSignOptions
    );

    return {
      token,
      user: {
        id: user.id,
        email: user.email,
        role,
        first_name: user.first_name,
        last_name: user.last_name,
      },
    };
  },

  async socialLogin(payload: SocialLoginPayload): Promise<{ token: string; user: User }> {
    const provider = normalizeSocialProvider(payload.provider);
    const platform = normalizePlatform(payload.platform);
    const email = String(payload.email || '').trim().toLowerCase();

    if (!email) {
      throw { status: 400, message: 'Email is required for social login' };
    }

    let user = await this.getUserByEmail(email);

    if (!user) {
      const firstName = payload.first_name?.trim() || provider;
      const lastName = payload.last_name?.trim() || 'User';

      // Production note: social flows should verify provider tokens server-side.
      user = await this.createUser({
        email,
        password: `oauth-${provider.toLowerCase()}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
        role: 'CAREGIVER',
        first_name: firstName,
        last_name: lastName,
      });
    }

    const role = normalizeUserRole(user.role, 'CAREGIVER');
    assertPlatformRoleAccess(platform, role);

    await this.createCaregiverProfileIfMissing(user);

    const token = jwt.sign(
      {
        userId: user.id,
        email: user.email,
        role,
      },
      jwtSecret,
      jwtSignOptions
    );

    return {
      token,
      user: {
        ...user,
        role,
      },
    };
  },

  async createUser(payload: {
    email: string;
    password: string;
    role: UserRole;
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
       RETURNING id, email, role, first_name, last_name, phone`,
      [email, passwordHash, role, first_name, last_name, phone]
    );

    const row = result.rows[0];

    return {
      id: row.id,
      email: row.email,
      role: normalizeUserRole(row.role, role),
      first_name: row.first_name,
      last_name: row.last_name,
      phone: row.phone,
    };
  },

  async createCaregiverProfileIfMissing(user: User): Promise<void> {
    if (user.role !== 'CAREGIVER') {
      return;
    }

    const existing = await query('SELECT id FROM caregivers WHERE user_id = $1 LIMIT 1', [user.id]);
    if (existing.rows.length > 0) {
      return;
    }

    const firstName = user.first_name?.trim() || 'Care';
    const lastName = user.last_name?.trim() || 'Giver';

    await query(
      `INSERT INTO caregivers (user_id, first_name, last_name, email, phone)
       VALUES ($1, $2, $3, $4, $5)`,
      [user.id, firstName, lastName, user.email, user.phone || null]
    );
  },

  async ensureEmailAvailable(email: string): Promise<void> {
    const existing = await query('SELECT id FROM users WHERE email = $1', [String(email || '').trim().toLowerCase()]);
    if (existing.rows.length > 0) {
      throw { status: 409, message: 'Email is already registered' };
    }
  },

  async register(payload: RegisterPayload): Promise<{ token: string; user: User }> {
    const normalizedEmail = String(payload.email || '').trim().toLowerCase();
    const { password, first_name, last_name, phone } = payload;
    const platform = normalizePlatform(payload.platform);
    const role = normalizeUserRole(payload.role, platform === 'WEB' ? 'CAREGIVER' : 'PATIENT');

    if (!PUBLIC_SIGNUP_ROLES.includes(role)) {
      throw { status: 403, message: 'Public signup supports CAREGIVER or PATIENT roles only' };
    }

    if (platform === 'WEB' && role !== 'CAREGIVER') {
      throw { status: 403, message: 'Web signup is restricted to CAREGIVER role' };
    }

    assertPlatformRoleAccess(platform, role);

    await this.ensureEmailAvailable(normalizedEmail);

    const createdUser = await this.createUser({
      email: normalizedEmail,
      password,
      role,
      first_name,
      last_name,
      phone,
    });

    await this.createCaregiverProfileIfMissing(createdUser);

    const token = jwt.sign(
      {
        userId: createdUser.id,
        email: createdUser.email,
        role: createdUser.role,
      },
      jwtSecret,
      jwtSignOptions
    );

    return {
      token,
      user: createdUser,
    };
  },

  async registerByAdmin(payload: RegisterPayload): Promise<User> {
    const normalizedEmail = String(payload.email || '').trim().toLowerCase();
    const { password, first_name, last_name, phone } = payload;
    const role = normalizeUserRole(payload.role, 'CAREGIVER');

    await this.ensureEmailAvailable(normalizedEmail);

    const createdUser = await this.createUser({
      email: normalizedEmail,
      password,
      role,
      first_name,
      last_name,
      phone,
    });

    await this.createCaregiverProfileIfMissing(createdUser);
    return createdUser;
  },

  async getUserById(userId: string): Promise<User | null> {
    const result = await query(
      'SELECT id, email, role, first_name, last_name, phone FROM users WHERE id = $1',
      [userId]
    );

    if (result.rows.length === 0) {
      return null;
    }

    const row = result.rows[0];

    return {
      id: row.id,
      email: row.email,
      role: normalizeUserRole(row.role, 'PATIENT'),
      first_name: row.first_name,
      last_name: row.last_name,
      phone: row.phone,
    };
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
