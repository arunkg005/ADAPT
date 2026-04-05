import bcrypt from 'bcrypt';
import { query } from './index.js';

// Sample data for testing
const sampleUsers = [
  {
    email: 'admin@adapt.local',
    password: 'password123',
    role: 'ADMIN',
    first_name: 'John',
    last_name: 'Administrator',
    phone: '+1-555-0001',
  },
  {
    email: 'caregiver1@adapt.local',
    password: 'password123',
    role: 'CAREGIVER',
    first_name: 'Sarah',
    last_name: 'Caregiver',
    phone: '+1-555-0002',
  },
  {
    email: 'caregiver2@adapt.local',
    password: 'password123',
    role: 'CAREGIVER',
    first_name: 'Michael',
    last_name: 'Helper',
    phone: '+1-555-0003',
  },
];

const samplePatients = [
  {
    first_name: 'Robert',
    last_name: 'Johnson',
    date_of_birth: '1960-05-12',
    cognitive_condition: 'AGE_RELATED',
    risk_level: 'MEDIUM',
  },
  {
    first_name: 'Emma',
    last_name: 'Williams',
    date_of_birth: '1958-03-25',
    cognitive_condition: 'DOWN_SYNDROME',
    risk_level: 'HIGH',
  },
  {
    first_name: 'David',
    last_name: 'Brown',
    date_of_birth: '1975-07-18',
    cognitive_condition: 'TBI',
    risk_level: 'MEDIUM',
  },
];

const sampleCaregivers = [
  {
    first_name: 'Sarah',
    last_name: 'Caregiver',
    email: 'sarah@example.com',
    phone: '+1-555-0002',
  },
  {
    first_name: 'Michael',
    last_name: 'Helper',
    email: 'michael@example.com',
    phone: '+1-555-0003',
  },
];

async function seed() {
  try {
    console.log('🌱 Seeding database with sample data...\n');

    // Create users
    console.log('📝 Creating users...');
    const userIds: any = {};
    for (const user of sampleUsers) {
      const passwordHash = await bcrypt.hash(user.password, 10);
      const result = await query(
        `INSERT INTO users (email, password_hash, role, first_name, last_name, phone)
         VALUES ($1, $2, $3, $4, $5, $6)
         ON CONFLICT (email) DO NOTHING
         RETURNING id`,
        [user.email, passwordHash, user.role, user.first_name, user.last_name, user.phone]
      );
      if (result.rows.length > 0) {
        userIds[user.email] = result.rows[0].id;
        console.log(`  ✅ Created user: ${user.email} (${user.role})`);
      }
    }

    // Create patients
    console.log('\n👥 Creating patients...');
    const patientIds: string[] = [];
    for (const patient of samplePatients) {
      const result = await query(
        `INSERT INTO patients (first_name, last_name, date_of_birth, cognitive_condition, risk_level)
         VALUES ($1, $2, $3, $4, $5)
         RETURNING id`,
        [
          patient.first_name,
          patient.last_name,
          patient.date_of_birth,
          patient.cognitive_condition,
          patient.risk_level,
        ]
      );
      patientIds.push(result.rows[0].id);
      console.log(`  ✅ Created patient: ${patient.first_name} ${patient.last_name} (${patient.cognitive_condition})`);
    }

    // Create caregivers
    console.log('\n🤝 Creating caregivers...');
    const caregiverIds: string[] = [];
    for (const caregiver of sampleCaregivers) {
      const result = await query(
        `INSERT INTO caregivers (first_name, last_name, email, phone)
         VALUES ($1, $2, $3, $4)
         RETURNING id`,
        [caregiver.first_name, caregiver.last_name, caregiver.email, caregiver.phone]
      );
      caregiverIds.push(result.rows[0].id);
      console.log(`  ✅ Created caregiver: ${caregiver.first_name} ${caregiver.last_name}`);
    }

    // Link caregivers to patients
    console.log('\n🔗 Linking caregivers to patients...');
    for (let i = 0; i < patientIds.length; i++) {
      const caregiverId = caregiverIds[i % caregiverIds.length];
      await query(
        `INSERT INTO patient_caregiver_links (patient_id, caregiver_id, relationship)
         VALUES ($1, $2, $3)
         ON CONFLICT (patient_id, caregiver_id) DO NOTHING`,
        [patientIds[i], caregiverId, 'PROFESSIONAL']
      );
      console.log(`  ✅ Linked patient ${i + 1} to caregiver ${(i % caregiverIds.length) + 1}`);
    }

    // Create sample devices
    console.log('\n📱 Creating devices...');
    for (let i = 0; i < patientIds.length; i++) {
      const result = await query(
        `INSERT INTO devices (patient_id, device_name, device_type, os, capabilities, is_online)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING id`,
        [
          patientIds[i],
          `Tablet-${i + 1}`,
          'TABLET',
          'ANDROID',
          ['GPS', 'MIC', 'ACCELEROMETER'],
          true,
        ]
      );
      console.log(`  ✅ Created device for patient ${i + 1}`);
    }

    console.log('\n✨ Database seeding completed successfully!\n');
    console.log('📚 Sample login credentials:');
    console.log(`  Email: admin@adapt.local`);
    console.log(`  Password: password123`);
    console.log(`\n  OR`);
    console.log(`  Email: caregiver1@adapt.local`);
    console.log(`  Password: password123`);

    process.exit(0);
  } catch (error) {
    console.error('❌ Seeding failed:', error);
    process.exit(1);
  }
}

seed();
