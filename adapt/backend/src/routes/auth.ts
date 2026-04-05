import { Router, Response } from 'express';
import { authService } from '../services/authService.js';
import { AuthRequest, authMiddleware } from '../middleware/auth.js';

const router = Router();

// POST /api/auth/login
router.post('/login', async (req: any, res: Response) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    const result = await authService.login({ email, password });
    res.json(result);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/auth/logout
router.post('/logout', (req: AuthRequest, res: Response) => {
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
    const { email, password, first_name, last_name, phone } = req.body;

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
    });

    res.status(201).json(result);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
