import { query } from './index.js';

const schema = `
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users Table
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL DEFAULT 'CAREGIVER',
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  phone VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Patients Table
CREATE TABLE IF NOT EXISTS patients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  date_of_birth DATE,
  cognitive_condition VARCHAR(255),
  risk_level VARCHAR(50) DEFAULT 'MEDIUM',
  baseline_response_time_ms INT DEFAULT 3000,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patients_risk_level ON patients(risk_level);
CREATE INDEX IF NOT EXISTS idx_patients_condition ON patients(cognitive_condition);

-- Caregivers Table
CREATE TABLE IF NOT EXISTS caregivers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_caregivers_email ON caregivers(email);

-- Patient-Caregiver Links
CREATE TABLE IF NOT EXISTS patient_caregiver_links (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  caregiver_id UUID NOT NULL REFERENCES caregivers(id) ON DELETE CASCADE,
  relationship VARCHAR(100) DEFAULT 'PROFESSIONAL',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(patient_id, caregiver_id)
);

CREATE INDEX IF NOT EXISTS idx_pcl_patient_id ON patient_caregiver_links(patient_id);
CREATE INDEX IF NOT EXISTS idx_pcl_caregiver_id ON patient_caregiver_links(caregiver_id);

-- Devices Table
CREATE TABLE IF NOT EXISTS devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  device_name VARCHAR(255),
  device_type VARCHAR(50),
  os VARCHAR(50),
  capabilities TEXT[],
  is_online BOOLEAN DEFAULT FALSE,
  last_seen_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_devices_patient_id ON devices(patient_id);
CREATE INDEX IF NOT EXISTS idx_devices_is_online ON devices(is_online);

-- Alerts Table
CREATE TABLE IF NOT EXISTS alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  task_name VARCHAR(255),
  task_state VARCHAR(50),
  severity VARCHAR(50),
  primary_issue VARCHAR(100),
  assistance_mode VARCHAR(50),
  engine_output JSONB,
  is_acknowledged BOOLEAN DEFAULT FALSE,
  acknowledged_by UUID REFERENCES caregivers(id),
  acknowledged_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alerts_patient_id ON alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alerts_is_acknowledged ON alerts(is_acknowledged);
CREATE INDEX IF NOT EXISTS idx_alerts_created_at ON alerts(created_at DESC);

-- Telemetry Table
CREATE TABLE IF NOT EXISTS telemetry (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
  signal_type VARCHAR(100),
  signal_value JSONB,
  timestamp_ms BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Backward-compatible patch for earlier schemas missing telemetry.device_id
ALTER TABLE telemetry
ADD COLUMN IF NOT EXISTS device_id UUID REFERENCES devices(id) ON DELETE SET NULL;

-- Backward-compatible patch for earlier schemas using metric_type/metric_value
ALTER TABLE telemetry
ADD COLUMN IF NOT EXISTS signal_type VARCHAR(100);

ALTER TABLE telemetry
ADD COLUMN IF NOT EXISTS signal_value JSONB;

ALTER TABLE telemetry
ADD COLUMN IF NOT EXISTS timestamp_ms BIGINT;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'telemetry' AND column_name = 'metric_type'
  ) THEN
    EXECUTE 'UPDATE telemetry SET signal_type = COALESCE(signal_type, metric_type) WHERE signal_type IS NULL';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'telemetry' AND column_name = 'metric_value'
  ) THEN
    EXECUTE 'UPDATE telemetry SET signal_value = COALESCE(signal_value, metric_value) WHERE signal_value IS NULL';
  END IF;
END $$;

UPDATE telemetry
SET timestamp_ms = COALESCE(timestamp_ms, (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT)
WHERE timestamp_ms IS NULL;

CREATE INDEX IF NOT EXISTS idx_telemetry_patient_id ON telemetry(patient_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_device_id ON telemetry(device_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_created_at ON telemetry(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_timestamp_ms ON telemetry(timestamp_ms DESC);

-- Task Lab Plans (shared by web and mobile)
CREATE TABLE IF NOT EXISTS task_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  scheduled_time VARCHAR(64),
  task_type VARCHAR(50) DEFAULT 'OTHER',
  risk_level VARCHAR(50) DEFAULT 'MEDIUM',
  complexity VARCHAR(50) DEFAULT 'MEDIUM',
  status VARCHAR(30) DEFAULT 'DRAFT',
  source VARCHAR(30) DEFAULT 'MANUAL',
  template_key VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_plans_patient_id ON task_plans(patient_id);
CREATE INDEX IF NOT EXISTS idx_task_plans_status ON task_plans(status);
CREATE INDEX IF NOT EXISTS idx_task_plans_created_at ON task_plans(created_at DESC);

-- Task Lab Plan Steps
CREATE TABLE IF NOT EXISTS task_plan_steps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  plan_id UUID NOT NULL REFERENCES task_plans(id) ON DELETE CASCADE,
  step_order INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  details TEXT,
  is_required BOOLEAN DEFAULT TRUE,
  is_completed BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(plan_id, step_order)
);

CREATE INDEX IF NOT EXISTS idx_task_plan_steps_plan_id ON task_plan_steps(plan_id);
CREATE INDEX IF NOT EXISTS idx_task_plan_steps_completion ON task_plan_steps(is_completed);
`;

async function migrate() {
  try {
    console.log('🔄 Running database migrations...');
    
    // Execute schema
    await query(schema);
    
    console.log('✅ Database migration completed successfully!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Migration failed:', error);
    process.exit(1);
  }
}

migrate();
