import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config.js';

export type UserRole = 'ADMIN' | 'CAREGIVER' | 'PATIENT';

const normalizeRole = (role?: string): UserRole | null => {
  const normalized = String(role || '').trim().toUpperCase();
  if (normalized === 'ADMIN' || normalized === 'CAREGIVER' || normalized === 'PATIENT') {
    return normalized;
  }

  return null;
};

export interface AuthRequest extends Request {
  userId?: string;
  userRole?: string;
  userEmail?: string;
}

export const authMiddleware = (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Missing or invalid Authorization header' });
    }

    const token = authHeader.substring(7);
    
    const decoded = jwt.verify(token, config.jwt.secret) as {
      userId: string;
      email: string;
      role: string;
    };

    req.userId = decoded.userId;
    req.userEmail = decoded.email;
    req.userRole = decoded.role;

    next();
  } catch (error) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
};

export const adminOnly = (req: AuthRequest, res: Response, next: NextFunction) => {
  if (normalizeRole(req.userRole) !== 'ADMIN') {
    return res.status(403).json({ error: 'Admin access required' });
  }
  next();
};

export const requireRoles = (...allowedRoles: UserRole[]) => {
  const allowed = new Set<UserRole>(allowedRoles);

  return (req: AuthRequest, res: Response, next: NextFunction) => {
    const role = normalizeRole(req.userRole);
    if (!role) {
      return res.status(403).json({ error: 'Role is missing or invalid for this action' });
    }

    if (!allowed.has(role)) {
      return res.status(403).json({
        error: 'Insufficient role for this action',
        allowedRoles,
      });
    }

    next();
  };
};
