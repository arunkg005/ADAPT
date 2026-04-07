import { Router, Response } from 'express';
import { authService } from '../services/authService.js';
import { AuthRequest, authMiddleware, adminOnly } from '../middleware/auth.js';

const router = Router();

// POST /api/auth/login
router.post('/login', async (req: any, res: Response) => {
  try {
    const { email, password, platform } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    const result = await authService.login({ email, password, platform });
    res.json(result);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/auth/social-login
router.post('/social-login', async (req: any, res: Response) => {
  try {
    const { provider, email, first_name, last_name, platform } = req.body;

    if (!provider || !email) {
      return res.status(400).json({ error: 'provider and email are required' });
    }

    const result = await authService.socialLogin({
      provider,
      email,
      first_name,
      last_name,
      platform,
    });

    res.json(result);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/auth/logout
router.post('/logout', authMiddleware, (req: AuthRequest, res: Response) => {
  // In a real app, you'd invalidate the token (e.g., add to blacklist)
  res.json({ message: 'Logged out successfully' });
});

// GET /api/auth/verify
router.get('/verify', authMiddleware, async (req: AuthRequest, res: Response) => {
  const user = await authService.getUserById(req.userId!);

  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  res.json({ user });
});

// POST /api/auth/register (public self-service signup)
router.post('/register', async (req: any, res: Response) => {
  try {
    const { email, password, first_name, last_name, phone, role, platform } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    if (password.length < 8) {
      return res.status(400).json({ error: 'Password must be at least 8 characters' });
    }

    const result = await authService.register({
      email,
      password,
      first_name,
      last_name,
      phone,
      role,
      platform,
    });

    res.status(201).json(result);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/auth/register/admin (admin-provisioned signup)
router.post('/register/admin', authMiddleware, adminOnly, async (req: any, res: Response) => {
  try {
    const { email, password, first_name, last_name, phone, role } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    if (password.length < 8) {
      return res.status(400).json({ error: 'Password must be at least 8 characters' });
    }

    const user = await authService.registerByAdmin({
      email,
      password,
      first_name,
      last_name,
      phone,
      role,
    });

    res.status(201).json({ user });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
