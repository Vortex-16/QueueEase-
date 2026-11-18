const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_vikas";
const STAFF_UID = "staff_rajesh";
const CITIZEN_A = "citizen_ananya";
const CITIZEN_B = "citizen_other";
const DB_ID = "ai-studio-android-ee150ac6-2711-48a4-ad86-f37005007781";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTs(offsetMs = 1500) {
  return new Date(Date.now() - offsetMs);
}

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

async function seedCatalogAndRoles() {
  const now = pastTs(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore(DB_ID);
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "vikasharmy811@gmail.com",
      role: "ADMIN",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("centres").doc("centre_central_psk").set({
      name: "Central Passport Seva Kendra (PSK)",
      category: "CIVIL_ID",
      address: "Plot 14, Civic Center",
      latitude: 28.6139,
      longitude: 77.209,
      distanceKm: 1.8,
      operatingHours: "09:00 AM - 05:30 PM",
      phone: "+91 1800110011",
      activeCountersCount: 4,
      totalCountersCount: 4,
      currentCrowdLevel: "MODERATE",
      estimatedWaitMin: 12,
      estimatedWaitMax: 18,
      lastCrowdUpdateMinutesAgo: 2,
      crowdConfidencePercent: 95,
      bestTimeToVisit: "03:00 PM - 05:00 PM",
      virtualQueueOpen: true,
      availableServiceIds: ["srv_passport_renew"],
      rating: 4.8,
      totalReviewsCount: 120,
      visibility: "PUBLIC",
      updatedByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("services").doc("srv_passport_renew").set({
      category: "CIVIL_ID",
      title: "Passport Renewal & Re-Issue",
      code: "PSP-RNW-01",
      description: "Standard 36-page booklet renewal",
      officialFee: "$45.00",
      avgProcessingDurationMin: 18,
      eligibility: "Adult citizens",
      proceduralSteps: ["Verify documents", "Biometrics"],
      requiredDocuments: [{ id: "doc_1", name: "Old Passport", isMandatory: true }],
      available: true,
      cautionNote: "Carry originals",
      visibility: "PUBLIC",
      updatedByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("counters").doc("cnt_psk_1").set({
      centreId: "centre_central_psk",
      counterNumber: 1,
      name: "Counter 1 - Intake",
      assignedStaffName: "Officer Rajesh Kumar",
      active: true,
      handledServiceIds: ["srv_passport_renew"],
      visibility: "PUBLIC",
      updatedByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staff_profiles").doc("usr_staff_1").set({
      name: "Officer Rajesh Kumar",
      email: "rajesh.k@passport.gov.in",
      phone: "+91 9811009941",
      assignedCentreId: "centre_central_psk",
      assignedCounterId: "cnt_psk_1",
      department: "Document Scrutiny",
      pin: "1234",
      active: true,
      createdByUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staff_access").doc(STAFF_UID).set({
      userId: STAFF_UID,
      email: "rajesh.k@passport.gov.in",
      staffProfileId: "usr_staff_1",
      assignedCentreId: "centre_central_psk",
      assignedCounterId: "cnt_psk_1",
      active: true,
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedCatalogAndRoles();
  const unauthDb = testEnv.unauthenticatedContext().firestore(DB_ID);
  await assertFails(unauthDb.collection("centres").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("tokens").get());
  await assertFails(
    unauthDb.collection("users").doc(CITIZEN_A).set({
      userId: CITIZEN_A,
      displayName: "Sneak",
      email: "a@b.com",
      lastPortalRole: "CUSTOMER",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
});

test("Citizen profile isolation & Ghost Field / role-escalation rejection", async () => {
  const dbA = testEnv.authenticatedContext(CITIZEN_A, { email: "ananya@example.com", email_verified: true }).firestore(DB_ID);
  const dbB = testEnv.authenticatedContext(CITIZEN_B, { email: "other@example.com", email_verified: true }).firestore(DB_ID);

  await assertSucceeds(
    dbA.collection("users").doc(CITIZEN_A).set({
      userId: CITIZEN_A,
      displayName: "Dr. Ananya Sharma",
      email: "ananya@example.com",
      phone: "+91 9876543210",
      lastPortalRole: "CUSTOMER",
      createdAt: pastTs(5000),
      updatedAt: pastTs(5000),
    })
  );

  // Cross-user read/write blocked
  await assertFails(dbB.collection("users").doc(CITIZEN_A).get());
  await assertFails(dbB.collection("users").doc(CITIZEN_A).update({ displayName: "Hacked" }));

  // Ghost field & future timestamp rejected
  await assertFails(
    dbB.collection("users").doc(CITIZEN_B).set({
      userId: CITIZEN_B,
      displayName: "Hacker",
      email: "other@example.com",
      lastPortalRole: "CUSTOMER",
      isAdmin: true,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
  await assertFails(
    dbB.collection("users").doc(CITIZEN_B).set({
      userId: CITIZEN_B,
      displayName: "Future User",
      email: "other@example.com",
      lastPortalRole: "CUSTOMER",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: pastTs(),
    })
  );
  // Self-assigning Admin or Staff access rejected
  await assertFails(
    dbB.collection("admins").doc(CITIZEN_B).set({
      userId: CITIZEN_B,
      email: "other@example.com",
      role: "ADMIN",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
});

test("Bootstrapped Admin email_verified check & Admin catalog write parity", async () => {
  const spoofedAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: "vikasharmy811@gmail.com",
    email_verified: false,
  }).firestore(DB_ID);

  // Email spoofing without email_verified == true must FAIL
  await assertFails(
    spoofedAdmin.collection("admins").doc("spoof_uid").set({
      userId: "spoof_uid",
      email: "vikasharmy811@gmail.com",
      role: "ADMIN",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );

  // Verified bootstrapped admin succeeds
  const realAdmin = testEnv.authenticatedContext("real_admin_uid", {
    email: "vikasharmy811@gmail.com",
    email_verified: true,
  }).firestore(DB_ID);

  await assertSucceeds(
    realAdmin.collection("admins").doc("real_admin_uid").set({
      userId: "real_admin_uid",
      email: "vikasharmy811@gmail.com",
      role: "ADMIN",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
});

test("Citizen token lifecycle, filtered query enforcement, and PII isolation", async () => {
  await seedCatalogAndRoles();
  const citizenADb = testEnv.authenticatedContext(CITIZEN_A, { email: "ananya@example.com", email_verified: true }).firestore(DB_ID);
  const citizenBDb = testEnv.authenticatedContext(CITIZEN_B, { email: "other@example.com", email_verified: true }).firestore(DB_ID);

  await assertSucceeds(
    citizenADb.collection("tokens").doc("tkn_1").set({
      tokenNumber: "C-101",
      centreId: "centre_central_psk",
      centreName: "Central Passport Seva Kendra (PSK)",
      serviceId: "srv_passport_renew",
      serviceName: "Passport Renewal & Re-Issue",
      userId: CITIZEN_A,
      userName: "Dr. Ananya Sharma",
      userPhone: "+91 9876543210",
      status: "WAITING",
      positionInQueue: 1,
      peopleAhead: 0,
      estimatedWaitMin: 5,
      estimatedWaitMax: 10,
      joinedAtTimestamp: Date.now() - 2000,
      qrVerificationCode: "QEASE-TKN-C-101",
      createdAt: pastTs(4000),
      updatedAt: pastTs(4000),
    })
  );

  // Unfiltered tokens query fails for Citizen B; filtered own query succeeds for Citizen A
  await assertFails(citizenBDb.collection("tokens").get());
  await assertSucceeds(citizenADb.collection("tokens").where("userId", "==", CITIZEN_A).get());

  // Cross-user token read/cancel fails
  await assertFails(citizenBDb.collection("tokens").doc("tkn_1").get());
  await assertFails(
    citizenBDb.collection("tokens").doc("tkn_1").update({
      status: "CANCELLED",
      updatedAt: pastTs(),
    })
  );

  // Citizen A can cancel own waiting token, then terminal lock blocks further updates
  await assertSucceeds(
    citizenADb.collection("tokens").doc("tkn_1").update({
      status: "CANCELLED",
      updatedAt: pastTs(),
    })
  );
  await assertFails(
    citizenADb.collection("tokens").doc("tkn_1").update({
      status: "WAITING",
      updatedAt: pastTs(),
    })
  );
});

test("Staff->Admin Approval Gate: Staff creates PENDING proposal, cannot self-approve, Admin approves and publishes", async () => {
  await seedCatalogAndRoles();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, { email: "rajesh.k@passport.gov.in", email_verified: true }).firestore(DB_ID);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "vikasharmy811@gmail.com", email_verified: true }).firestore(DB_ID);

  // Staff submits PENDING proposal
  await assertSucceeds(
    staffDb.collection("service_change_proposals").doc("prop_1").set({
      proposalType: "SERVICE_INFO",
      serviceId: "srv_passport_renew",
      serviceTitle: "Passport Renewal & Re-Issue",
      centreId: "centre_central_psk",
      submittedByStaffId: "usr_staff_1",
      submittedByUid: STAFF_UID,
      submittedByStaffName: "Officer Rajesh Kumar",
      assignedCounterNumber: 1,
      rationale: "Updated biometric capture duration",
      status: "PENDING",
      submittedAt: Date.now() - 2000,
      adminReviewNote: "",
      previousFee: "$45.00",
      proposedFee: "$50.00",
      previousDurationMin: 18,
      proposedDurationMin: 14,
      previousDescription: "Standard 36-page booklet renewal",
      proposedDescription: "Fast-track 36-page booklet renewal",
      previousEligibility: "Adult citizens",
      proposedEligibility: "Adult citizens",
      previousCautionNote: "Carry originals",
      proposedCautionNote: "Carry originals & receipt",
      documentId: "",
      previousDocName: "",
      proposedDocName: "",
      previousDocDescription: "",
      proposedDocDescription: "",
      previousDocMandatory: true,
      proposedDocMandatory: true,
      previousDocAlternatives: [],
      proposedDocAlternatives: [],
      createdAt: pastTs(3000),
      updatedAt: pastTs(3000),
    })
  );

  // Staff CANNOT approve own proposal or mutate live service catalog directly
  await assertFails(
    staffDb.collection("service_change_proposals").doc("prop_1").update({
      status: "APPROVED",
      reviewedAt: Date.now(),
      reviewedByAdminName: "Officer Rajesh Kumar",
      adminReviewNote: "Self approved",
      updatedAt: pastTs(),
    })
  );
  await assertFails(
    staffDb.collection("services").doc("srv_passport_renew").update({
      officialFee: "$50.00",
      updatedAt: pastTs(),
    })
  );

  // Admin approves proposal & updates live service catalog
  await assertSucceeds(
    adminDb.collection("service_change_proposals").doc("prop_1").update({
      status: "APPROVED",
      reviewedAt: Date.now(),
      reviewedByAdminName: "Chief Officer Vikramaditya",
      adminReviewNote: "Verified circular",
      updatedAt: pastTs(),
    })
  );
  await assertSucceeds(
    adminDb.collection("services").doc("srv_passport_renew").update({
      officialFee: "$50.00",
      avgProcessingDurationMin: 14,
      updatedByUid: ADMIN_UID,
      updatedAt: pastTs(),
    })
  );
});

test("Public catalog and community queries enforce visibility == PUBLIC", async () => {
  await seedCatalogAndRoles();
  const citizenDb = testEnv.authenticatedContext(CITIZEN_A, { email: "ananya@example.com", email_verified: true }).firestore(DB_ID);

  await assertSucceeds(citizenDb.collection("centres").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(citizenDb.collection("services").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(citizenDb.collection("counters").where("visibility", "==", "PUBLIC").get());
  await assertFails(citizenDb.collection("staff_profiles").get());
});
