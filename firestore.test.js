const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_prof_123";
const BOB_UID = "bob_prof_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS & ISOLATION TESTS ---

test("Unauthenticated user: cannot read courses or profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("courses").get());
});

test("Authenticated user: can create and read their own TeacherProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Dr. Alice Smith",
      email: "alice@university.edu",
      primaryDepartment: "Computer Science",
      subjects: ["Data Structures", "Algorithms"],
      departments: ["Computer Science", "Software Engineering"],
      createdAt: new Date(),
    })
  );

  const doc = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user isolation: Bob cannot read Alice's profile or courses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Dr. Alice Smith",
      primaryDepartment: "Computer Science",
      subjects: ["Data Structures"],
      departments: ["Computer Science"],
      createdAt: new Date(),
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("courses").doc("c1").set({
      userId: ALICE_UID,
      courseCode: "CS-301",
      courseTitle: "Data Structures",
      department: "Computer Science",
      section: "Sec A",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("courses").doc("c1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("courses").get());
});

test("Course operations: Alice can create and list courses in her section", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("courses").doc("cs301_a").set({
      userId: ALICE_UID,
      courseCode: "CS-301",
      courseTitle: "Data Structures & Algorithms",
      department: "Computer Science",
      section: "Section A",
      creditHours: 3,
      semester: "Fall 2026",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("courses").get()
  );
});

test("Student, Assessment, Score & Attendance creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Create Student
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("students").doc("s1").set({
      userId: ALICE_UID,
      courseId: "cs301_a",
      department: "Computer Science",
      section: "Section A",
      rollNumber: "CS-2023-01",
      name: "John Doe",
      email: "john@student.edu",
      assignmentQuizTotal: 85,
      midtermScore: 78,
      finalTermScore: 88,
      totalAttendanceCount: 18,
      totalAttendancePossible: 20,
      createdAt: new Date(),
    })
  );

  // Create Assessment
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assessments").doc("a1").set({
      userId: ALICE_UID,
      courseId: "cs301_a",
      title: "Quiz 1 - Stacks",
      type: "QUIZ",
      maxMarks: 20,
      weightPercentage: 5,
      date: "2026-10-21",
      createdAt: new Date(),
    })
  );

  // Create Score
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scores").doc("s1_a1").set({
      userId: ALICE_UID,
      courseId: "cs301_a",
      studentId: "s1",
      assessmentId: "a1",
      obtainedMarks: 18,
      submitted: true,
      submissionNotes: "Well done",
      createdAt: new Date(),
    })
  );

  // Create Attendance
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance_records").doc("att1").set({
      userId: ALICE_UID,
      courseId: "cs301_a",
      date: "2026-10-21",
      topic: "Introduction to Stacks & Queues",
      studentStatuses: { s1: "PRESENT" },
      createdAt: new Date(),
    })
  );
});

test("Schema validation: rejects invalid assessment type", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("assessments").doc("bad_type").set({
      userId: ALICE_UID,
      courseId: "cs301_a",
      title: "Invalid Type Assessment",
      type: "UNKNOWN_TYPE",
      maxMarks: 20,
      createdAt: new Date(),
    })
  );
});
