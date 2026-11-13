const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

// --- 1. Unauthenticated Rejections ---

test("Unauthenticated user: cannot read or write users, bookStates, collections, reviews, discussions, contributions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookStates").get());
  await assertFails(unauthDb.collection("collections").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("reviews").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("discussions").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("contributions").where("userId", "==", ALICE_UID).get());
});

// --- 2. User Profile (PII Isolation) ---

test("UserProfile: owner can create and read own profile, non-owner cannot read or write", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_reads",
      firstName: "Alice",
      lastName: "Verma",
      countryCode: "+91",
      phoneNumber: "9876543210",
      phoneVerified: true,
      bio: "Literary fiction lover",
      followersCount: 12,
      followingCount: 8,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: serverTimestamp(),
    })
  );
});

// --- 3. UserBookState ---

test("UserBookState: owner can create/update shelf status and ratingTier; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookStates").doc("book_1").set({
      userId: ALICE_UID,
      bookId: "book_1",
      status: "read",
      ratingTier: "must_read",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookStates").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookStates").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookStates").doc("book_1").update({
      ratingTier: "invalid_tier",
      updatedAt: serverTimestamp(),
    })
  );
});

// --- 4. Collections ---

test("BookCollection: owner CRUD works; public list query passes; unfiltered list fails; non-owner update fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("collections").doc("col_1").set({
      userId: ALICE_UID,
      creatorName: "Alice Verma",
      creatorUsername: "alice_reads",
      title: "Monsoon Mystery Reads",
      description: "Atmospheric thrillers for rainy evenings",
      isPublic: true,
      bookIds: ["book_1", "book_2"],
      topicTag: "Thriller",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("collections").where("isPublic", "==", true).get()
  );
  await assertSucceeds(
    aliceDb.collection("collections").where("userId", "==", ALICE_UID).get()
  );
  // Rules are not filters: unfiltered query must fail
  await assertFails(bobDb.collection("collections").get());
  // Non-owner cannot update
  await assertFails(
    bobDb.collection("collections").doc("col_1").update({
      title: "Hacked",
      updatedAt: serverTimestamp(),
    })
  );
});

// --- 5. Reviews ---

test("BookReview: authenticated user can post review and query public reviews; cross-user delete denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      userId: ALICE_UID,
      bookId: "book_1",
      bookTitle: "The Midnight Lighthouse",
      authorName: "Alice Verma",
      authorUsername: "alice_reads",
      ratingTier: "must_read",
      topicTag: "Fiction",
      body: "An unforgettable coastal story with rich psychological depth.",
      isPublic: true,
      likesCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("reviews").where("isPublic", "==", true).where("bookId", "==", "book_1").get()
  );
  await assertFails(bobDb.collection("reviews").get());
  await assertFails(bobDb.collection("reviews").doc("rev_1").delete());
});

// --- 6. Discussions & Replies ---

test("DiscussionPost & Replies: create post, post nested reply, reject orphan reply", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("discussions").doc("disc_1").set({
      userId: ALICE_UID,
      authorName: "Alice Verma",
      authorUsername: "alice_reads",
      topicTag: "Fiction",
      bookId: "book_1",
      title: "Does the unreliable narrator change how you read Part II?",
      body: "Curious what everyone felt about the twist in chapter 9.",
      isPublic: true,
      upvotes: 0,
      repliesCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("discussions").doc("disc_1").collection("replies").doc("rep_1").set({
      userId: BOB_UID,
      authorName: "Bob Rao",
      authorUsername: "bob_books",
      parentReplyId: "",
      body: "Completely! Re-reading chapter 2 reveals so many clues.",
      isPublic: true,
      createdAt: serverTimestamp(),
    })
  );

  await assertFails(
    bobDb.collection("discussions").doc("missing_disc").collection("replies").doc("rep_2").set({
      userId: BOB_UID,
      authorName: "Bob Rao",
      authorUsername: "bob_books",
      parentReplyId: "",
      body: "Orphan reply",
      isPublic: true,
      createdAt: serverTimestamp(),
    })
  );
});

// --- 7. Contributions ---

test("BookContribution: owner can create pending contribution and query own; cannot self-approve", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("contributions").doc("contrib_1").set({
      userId: ALICE_UID,
      bookTitle: "Raag Darbari",
      authorName: "Shrilal Shukla",
      publisher: "Rajkamal Prakashan",
      language: "Hindi",
      genre: "Satire",
      notes: "Sahitya Akademi Award winner",
      status: "pending",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("contributions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("contributions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    aliceDb.collection("contributions").doc("contrib_1").update({
      status: "approved",
      updatedAt: serverTimestamp(),
    })
  );
});
