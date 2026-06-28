const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 8080;

app.use(cors());
app.use(express.json());

// Request logger middleware
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    if (req.method !== 'GET' && Object.keys(req.body).length > 0) {
        console.log('Body:', JSON.stringify(req.body, null, 2));
    }
    next();
});

// --- Initialize Firebase Admin SDK ---
let db = null;
let useFirebase = false;
const serviceAccountPath = path.join(__dirname, 'firebase-service-account.json');

if (fs.existsSync(serviceAccountPath)) {
    try {
        const admin = require('firebase-admin');
        const serviceAccount = require(serviceAccountPath);
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
        });
        db = admin.firestore();
        useFirebase = true;
        console.log(`\n===================================================================`);
        console.log(`[INFO] Firebase Admin initialized successfully using service account key!`);
        console.log(`[INFO] Connection to Firestore established. Active project: ${serviceAccount.project_id}`);
        console.log(`===================================================================\n`);
    } catch (e) {
        console.error(`\n[ERROR] Failed to initialize Firebase Admin SDK:`, e);
    }
} else {
    console.log(`\n===================================================================`);
    console.log(`[WARNING] firebase-service-account.json not found in mock-server/ directory.`);
    console.log(`          Running in local IN-MEMORY mode. Data will NOT be synchronized to Firebase.`);
    console.log(`          To enable Firebase integration, download your Service Account JSON file`);
    console.log(`          from Firebase Console -> Project Settings -> Service Accounts`);
    console.log(`          and place it in:`);
    console.log(`          ${serviceAccountPath}`);
    console.log(`===================================================================\n`);
}

// --- Firebase Web API Key for Auth Token Exchange ---
const FIREBASE_API_KEY = "AIzaSyDEEgIXZjWGM41mO3EKgFryTNo-CwSXNrU";

// Exchanges Google ID token for Firebase Auth ID token and returns the REAL UID
async function getFirebaseUserFromGoogleToken(idToken) {
    try {
        const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=${FIREBASE_API_KEY}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                postBody: `id_token=${idToken}&providerId=google.com`,
                requestUri: "http://localhost",
                returnIdpCredential: true,
                returnSecureToken: true
            })
        });
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(`Firebase Auth API error: ${errText}`);
        }
        const data = await response.json();
        return {
            uid: data.localId,
            email: data.email,
            displayName: data.displayName,
            photoUrl: data.photoUrl
        };
    } catch (e) {
        console.error("Failed to exchange Google token for Firebase token:", e);
        return null;
    }
}

// Signs in with Email/Password on Firebase Auth to get the REAL UID
async function getFirebaseUserFromEmailPassword(email, password) {
    try {
        const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${FIREBASE_API_KEY}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email,
                password,
                returnSecureToken: true
            })
        });
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(`Firebase Auth API error: ${errText}`);
        }
        const data = await response.json();
        return {
            uid: data.localId,
            email: data.email,
            displayName: data.displayName,
            photoUrl: data.photoUrl
        };
    } catch (e) {
        console.error("Failed to login with email/password on Firebase:", e);
        return null;
    }
}

// Registers user on Firebase Auth and returns the REAL UID
async function registerFirebaseUserWithEmailPassword(email, password, displayName) {
    try {
        const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FIREBASE_API_KEY}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email,
                password,
                returnSecureToken: true
            })
        });
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(`Firebase Auth API error: ${errText}`);
        }
        const data = await response.json();
        const uid = data.localId;
        
        // Update display name on Firebase Auth if provided
        if (displayName) {
            await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:update?key=${FIREBASE_API_KEY}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    idToken: data.idToken,
                    displayName,
                    returnSecureToken: true
                })
            });
        }
        
        return {
            uid,
            email: data.email,
            displayName: displayName || data.email.split('@')[0],
            photoUrl: null
        };
    } catch (e) {
        console.error("Failed to register with email/password on Firebase:", e);
        return null;
    }
}

// --- In-Memory database (Fallback when Firebase is not connected) ---
const inMemoryDb = {
    users: {
        "user_default": {
            id: "user_default",
            userId: "user_default",
            name: "Người dùng NewStart",
            email: "user@example.com",
            avatarUrl: "https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg"
        },
        "user_alice": {
            id: "user_alice",
            userId: "user_alice",
            name: "Alice Smith",
            email: "alice@example.com",
            avatarUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"
        },
        "user_bob": {
            id: "user_bob",
            userId: "user_bob",
            name: "Bob Johnson",
            email: "bob@example.com",
            avatarUrl: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
        }
    },
    habits: [],
    journals: [],
    todos: [],
    friendships: [],
    friendRequests: [],
    squads: [],
    squadMessages: []
};

// Helper to get or create in-memory user by email
function getOrCreateInMemoryUser(email, name = null, customId = null) {
    const searchId = customId;
    if (searchId && inMemoryDb.users[searchId]) {
        return inMemoryDb.users[searchId];
    }
    let user = Object.values(inMemoryDb.users).find(u => u.email.toLowerCase() === email.toLowerCase());
    if (!user) {
        const id = customId || "user_" + Math.random().toString(36).substr(2, 9);
        user = {
            id: id,
            userId: id,
            name: name || email.split('@')[0],
            email: email,
            avatarUrl: `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(email)}`
        };
        inMemoryDb.users[id] = user;
    }
    return user;
}

// --- API Endpoints ---

// --- Authentication ---
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body;
    if (!email) return res.status(400).json({ error: "Email is required" });

    let uid = null;
    let name = email.split('@')[0];
    let avatarUrl = null;

    // Authenticate with Firebase Auth to get the REAL UID
    const firebaseUser = await getFirebaseUserFromEmailPassword(email, password);
    if (firebaseUser) {
        uid = firebaseUser.uid;
        name = firebaseUser.displayName || name;
        avatarUrl = firebaseUser.photoUrl || avatarUrl;
    }

    const finalUid = uid || "user_" + Math.random().toString(36).substr(2, 9);

    if (useFirebase) {
        try {
            const userRef = db.collection('users').doc(finalUid);
            const userDoc = await userRef.get();
            const userData = {
                id: finalUid,
                userId: finalUid,
                name: name,
                email: email,
                avatarUrl: avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(email)}`
            };
            if (!userDoc.exists) {
                await userRef.set(userData);
            }
            const doc = await userRef.get();
            return res.json({ id: doc.id, ...doc.data() });
        } catch (e) {
            console.error("Firestore auth write error:", e);
        }
    }

    const user = getOrCreateInMemoryUser(email, name, finalUid);
    res.json(user);
});

app.post('/api/auth/register', async (req, res) => {
    const { email, name, password } = req.body;
    if (!email) return res.status(400).json({ error: "Email is required" });

    let uid = null;
    let avatarUrl = null;

    // Register with Firebase Auth to get the REAL UID
    const firebaseUser = await registerFirebaseUserWithEmailPassword(email, password, name);
    if (firebaseUser) {
        uid = firebaseUser.uid;
        avatarUrl = firebaseUser.photoUrl || avatarUrl;
    }

    const finalUid = uid || "user_" + Math.random().toString(36).substr(2, 9);

    if (useFirebase) {
        try {
            const userRef = db.collection('users').doc(finalUid);
            const userData = {
                id: finalUid,
                userId: finalUid,
                name: name || email.split('@')[0],
                email: email,
                avatarUrl: avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(email)}`
            };
            await userRef.set(userData);
            return res.json(userData);
        } catch (e) {
            console.error("Firestore register write error:", e);
        }
    }

    const user = getOrCreateInMemoryUser(email, name, finalUid);
    res.json(user);
});

app.post('/api/auth/google', async (req, res) => {
    const { idToken } = req.body;
    let email = "google_user@example.com";
    let name = "Google User";
    let avatarUrl = null;
    let uid = null;

    if (idToken) {
        const firebaseUser = await getFirebaseUserFromGoogleToken(idToken);
        if (firebaseUser) {
            uid = firebaseUser.uid;
            email = firebaseUser.email || email;
            name = firebaseUser.displayName || name;
            avatarUrl = firebaseUser.photoUrl || avatarUrl;
            console.log(`[AUTH] Successfully authenticated Google Token against Firebase. Real UID: ${uid}`);
        } else {
            try {
                const parts = idToken.split('.');
                if (parts.length >= 2) {
                    const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf8'));
                    if (payload.email) email = payload.email;
                    if (payload.name) name = payload.name;
                    if (payload.picture) avatarUrl = payload.picture;
                }
            } catch (e) {
                console.error("Failed to parse Google idToken locally:", e);
            }
        }
    }

    const finalUid = uid || "user_" + Math.random().toString(36).substr(2, 9);

    if (useFirebase) {
        try {
            const userRef = db.collection('users').doc(finalUid);
            const userDoc = await userRef.get();
            const userData = {
                id: finalUid,
                userId: finalUid,
                name: name,
                email: email,
                avatarUrl: avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(email)}`
            };
            if (!userDoc.exists) {
                await userRef.set(userData);
            } else {
                const updates = {};
                if (name) updates.name = name;
                if (email) updates.email = email;
                if (avatarUrl) updates.avatarUrl = avatarUrl;
                if (Object.keys(updates).length > 0) {
                    await userRef.update(updates);
                }
            }
            const doc = await userRef.get();
            return res.json({ id: doc.id, ...doc.data() });
        } catch (e) {
            console.error("Firestore Google Auth database write error:", e);
        }
    }

    let user = inMemoryDb.users[finalUid];
    if (!user) {
        user = {
            id: finalUid,
            userId: finalUid,
            name: name,
            email: email,
            avatarUrl: avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(email)}`
        };
        inMemoryDb.users[finalUid] = user;
    } else {
        if (name) user.name = name;
        if (avatarUrl) user.avatarUrl = avatarUrl;
    }
    res.json(user);
});

app.post('/api/auth/reset-password', (req, res) => {
    res.sendStatus(200);
});

app.post('/api/auth/send-verification', (req, res) => {
    res.sendStatus(200);
});

// --- User Profile ---
app.get('/api/users/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            const doc = await db.collection('users').doc(id).get();
            if (doc.exists) {
                return res.json({ id: doc.id, ...doc.data() });
            } else {
                const newUser = {
                    id: id,
                    userId: id,
                    name: "Người dùng NewStart",
                    email: "user@example.com",
                    avatarUrl: `https://api.dicebear.com/7.x/adventurer/svg?seed=${id}`
                };
                await db.collection('users').doc(id).set(newUser);
                return res.json(newUser);
            }
        } catch (e) {
            console.error("Firestore getUser error:", e);
        }
    }

    let user = inMemoryDb.users[id];
    if (!user) {
        user = {
            id: id,
            userId: id,
            name: "Người dùng NewStart",
            email: "user@example.com",
            avatarUrl: `https://api.dicebear.com/7.x/adventurer/svg?seed=${id}`
        };
        inMemoryDb.users[id] = user;
    }
    res.json(user);
});

app.put('/api/users/:id/avatar', async (req, res) => {
    const { avatarUrl } = req.body;
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('users').doc(id).update({ avatarUrl });
            const doc = await db.collection('users').doc(id).get();
            return res.json({ id: doc.id, ...doc.data() });
        } catch (e) {
            console.error("Firestore updateAvatar error:", e);
        }
    }

    const user = inMemoryDb.users[id];
    if (user) {
        user.avatarUrl = avatarUrl;
        res.json(user);
    } else {
        res.status(404).json({ error: "User not found" });
    }
});

app.put('/api/users/:id/profile', async (req, res) => {
    const { name, email } = req.body;
    const id = req.params.id;

    if (useFirebase) {
        try {
            const updates = {};
            if (name) updates.name = name;
            if (email) updates.email = email;
            await db.collection('users').doc(id).update(updates);
            const doc = await db.collection('users').doc(id).get();
            return res.json({ id: doc.id, ...doc.data() });
        } catch (e) {
            console.error("Firestore updateProfile error:", e);
        }
    }

    const user = inMemoryDb.users[id];
    if (user) {
        if (name) user.name = name;
        if (email) user.email = email;
        res.json(user);
    } else {
        res.status(404).json({ error: "User not found" });
    }
});

app.get('/api/users/search', async (req, res) => {
    const { query } = req.query;
    if (!query) return res.json([]);

    if (useFirebase) {
        try {
            const snapshot = await db.collection('users').get();
            const matched = [];
            snapshot.forEach(doc => {
                const data = doc.data();
                const name = data.name || '';
                const email = data.email || '';
                if (name.toLowerCase().includes(query.toLowerCase()) || email.toLowerCase().includes(query.toLowerCase())) {
                    matched.push({ id: doc.id, ...data });
                }
            });
            return res.json(matched);
        } catch (e) {
            console.error("Firestore searchUsers error:", e);
        }
    }

    const matched = Object.values(inMemoryDb.users).filter(u => 
        u.name.toLowerCase().includes(query.toLowerCase()) || 
        u.email.toLowerCase().includes(query.toLowerCase())
    );
    res.json(matched);
});

// --- Habits ---
app.get('/api/habits', async (req, res) => {
    const { date, userId } = req.query;

    if (useFirebase) {
        try {
            let queryRef = db.collection('habits').where('userId', '==', userId);
            if (date) {
                queryRef = queryRef.where('date', '==', date);
            }
            const snapshot = await queryRef.get();
            const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            return res.json(list);
        } catch (e) {
            console.error("Firestore getHabits error:", e);
        }
    }

    let filtered = inMemoryDb.habits.filter(h => h.userId === userId);
    if (date) {
        filtered = filtered.filter(h => h.date === date);
    }
    res.json(filtered);
});

app.get('/api/habits/all', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('habits').where('userId', '==', userId).get();
            const list = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            return res.json(list);
        } catch (e) {
            console.error("Firestore getAllHabits error:", e);
        }
    }

    const filtered = inMemoryDb.habits.filter(h => h.userId === userId);
    res.json(filtered);
});

app.post('/api/habits', async (req, res) => {
    const habit = req.body;
    const id = habit.id || "habit_" + Math.random().toString(36).substr(2, 9);
    habit.id = id;
    habit.createdAt = habit.createdAt || new Date().toISOString();

    if (useFirebase) {
        try {
            await db.collection('habits').doc(id).set(habit);
            return res.status(201).json(habit);
        } catch (e) {
            console.error("Firestore saveHabit error:", e);
        }
    }

    inMemoryDb.habits.push(habit);
    res.status(201).json(habit);
});

app.put('/api/habits/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('habits').doc(id).set(req.body, { merge: true });
            const doc = await db.collection('habits').doc(id).get();
            return res.json({ id: doc.id, ...doc.data() });
        } catch (e) {
            console.error("Firestore updateHabit error:", e);
        }
    }

    const index = inMemoryDb.habits.findIndex(h => h.id === id);
    if (index !== -1) {
        inMemoryDb.habits[index] = { ...inMemoryDb.habits[index], ...req.body };
        res.json(inMemoryDb.habits[index]);
    } else {
        res.status(404).json({ error: "Habit not found" });
    }
});

app.delete('/api/habits/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('habits').doc(id).delete();
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore deleteHabit error:", e);
        }
    }

    const index = inMemoryDb.habits.findIndex(h => h.id === id);
    if (index !== -1) {
        inMemoryDb.habits.splice(index, 1);
    }
    res.sendStatus(200);
});

app.put('/api/habits/:id/toggle', async (req, res) => {
    const id = req.params.id;
    const { completed } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('habits').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const nextVal = completed !== undefined ? completed : !doc.data().isCompleted;
                await docRef.update({ isCompleted: nextVal });
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore toggleHabit error:", e);
        }
    }

    const habit = inMemoryDb.habits.find(h => h.id === id);
    if (habit) {
        habit.isCompleted = completed !== undefined ? completed : !habit.isCompleted;
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Habit not found" });
    }
});

// --- Journal ---
app.get('/api/journals', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('journals').where('userId', '==', userId).get();
            const list = snapshot.docs.map(doc => {
                const data = doc.data();
                if (data.timestamp && typeof data.timestamp.toDate === 'function') {
                    data.timestamp = data.timestamp.toDate().toISOString();
                }
                return { id: doc.id, ...data };
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getJournals error:", e);
        }
    }

    const filtered = inMemoryDb.journals.filter(j => j.userId === userId);
    res.json(filtered);
});

app.post('/api/journals', async (req, res) => {
    const entry = req.body;
    const id = entry.id || "journal_" + Math.random().toString(36).substr(2, 9);
    entry.id = id;
    entry.reactions = entry.reactions || {};
    entry.timestamp = entry.timestamp || new Date().toISOString();

    if (useFirebase) {
        try {
            const dbEntry = { ...entry };
            dbEntry.timestamp = new Date(entry.timestamp);
            await db.collection('journals').doc(id).set(dbEntry);
            return res.status(201).json(entry);
        } catch (e) {
            console.error("Firestore saveJournal error:", e);
        }
    }

    inMemoryDb.journals.push(entry);
    res.status(201).json(entry);
});

app.delete('/api/journals/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('journals').doc(id).delete();
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore deleteJournal error:", e);
        }
    }

    const index = inMemoryDb.journals.findIndex(j => j.id === id);
    if (index !== -1) {
        inMemoryDb.journals.splice(index, 1);
    }
    res.sendStatus(200);
});

// --- Todos ---
app.get('/api/todos', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('todos').where('userId', '==', userId).get();
            const list = snapshot.docs.map(doc => {
                const data = doc.data();
                if (data.dueDate && typeof data.dueDate.toDate === 'function') {
                    data.dueDate = data.dueDate.toDate().toISOString();
                }
                if (data.createdAt && typeof data.createdAt.toDate === 'function') {
                    data.createdAt = data.createdAt.toDate().toISOString();
                }
                return { id: doc.id, ...data };
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getTodos error:", e);
        }
    }

    const filtered = inMemoryDb.todos.filter(t => t.userId === userId);
    res.json(filtered);
});

app.post('/api/todos', async (req, res) => {
    const todo = req.body;
    const id = todo.id || "todo_" + Math.random().toString(36).substr(2, 9);
    todo.id = id;
    todo.createdAt = todo.createdAt || new Date().toISOString();

    if (useFirebase) {
        try {
            const dbTodo = { ...todo };
            if (todo.dueDate) dbTodo.dueDate = new Date(todo.dueDate);
            dbTodo.createdAt = new Date(todo.createdAt);
            await db.collection('todos').doc(id).set(dbTodo);
            return res.status(201).json(todo);
        } catch (e) {
            console.error("Firestore saveTodo error:", e);
        }
    }

    inMemoryDb.todos.push(todo);
    res.status(201).json(todo);
});

app.put('/api/todos/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            const dbTodoUpdates = { ...req.body };
            if (req.body.dueDate) dbTodoUpdates.dueDate = new Date(req.body.dueDate);
            await db.collection('todos').doc(id).set(dbTodoUpdates, { merge: true });
            
            const doc = await db.collection('todos').doc(id).get();
            const data = doc.data();
            if (data.dueDate && typeof data.dueDate.toDate === 'function') {
                data.dueDate = data.dueDate.toDate().toISOString();
            }
            if (data.createdAt && typeof data.createdAt.toDate === 'function') {
                data.createdAt = data.createdAt.toDate().toISOString();
            }
            return res.json({ id: doc.id, ...data });
        } catch (e) {
            console.error("Firestore updateTodo error:", e);
        }
    }

    const index = inMemoryDb.todos.findIndex(t => t.id === id);
    if (index !== -1) {
        inMemoryDb.todos[index] = { ...inMemoryDb.todos[index], ...req.body };
        res.json(inMemoryDb.todos[index]);
    } else {
        res.status(404).json({ error: "Todo not found" });
    }
});

app.delete('/api/todos/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('todos').doc(id).delete();
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore deleteTodo error:", e);
        }
    }

    const index = inMemoryDb.todos.findIndex(t => t.id === id);
    if (index !== -1) {
        inMemoryDb.todos.splice(index, 1);
    }
    res.sendStatus(200);
});

app.put('/api/todos/:id/toggle', async (req, res) => {
    const id = req.params.id;
    const { completed } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('todos').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const nextVal = completed !== undefined ? completed : !doc.data().isCompleted;
                await docRef.update({ isCompleted: nextVal });
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore toggleTodo error:", e);
        }
    }

    const todo = inMemoryDb.todos.find(t => t.id === id);
    if (todo) {
        todo.isCompleted = completed !== undefined ? completed : !todo.isCompleted;
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Todo not found" });
    }
});

// --- Social / Friends ---
app.get('/api/social/friends', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('friendships').get();
            const list = [];
            snapshot.forEach(doc => {
                const data = doc.data();
                const userIds = data.userIds || [];
                if (userIds.includes(userId)) {
                    if (data.createdAt && typeof data.createdAt.toDate === 'function') {
                        data.createdAt = data.createdAt.toDate().toISOString();
                    }
                    list.push({ id: doc.id, ...data });
                }
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getFriends error:", e);
        }
    }

    const list = inMemoryDb.friendships.filter(f => f.userIds.includes(userId));
    res.json(list);
});

app.post('/api/social/friend-requests', async (req, res) => {
    const { fromUserId, toUserId } = req.body;
    const reqId = "req_" + Math.random().toString(36).substr(2, 9);

    if (useFirebase) {
        try {
            await db.collection('friendRequests').doc(reqId).set({
                fromUserId,
                toUserId,
                status: "PENDING",
                timestamp: new Date()
            });
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore sendFriendRequest error:", e);
        }
    }

    inMemoryDb.friendRequests.push({
        id: reqId,
        fromUserId,
        toUserId,
        status: "PENDING",
        timestamp: new Date().toISOString()
    });
    res.sendStatus(200);
});

app.get('/api/social/incoming-requests', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('friendRequests')
                .where('toUserId', '==', userId)
                .where('status', '==', 'PENDING')
                .get();
            const list = snapshot.docs.map(doc => {
                const data = doc.data();
                if (data.timestamp && typeof data.timestamp.toDate === 'function') {
                    data.timestamp = data.timestamp.toDate().toISOString();
                }
                return { id: doc.id, ...data };
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getIncomingRequests error:", e);
        }
    }

    const filtered = inMemoryDb.friendRequests.filter(r => r.toUserId === userId && r.status === "PENDING");
    res.json(filtered);
});

app.get('/api/social/sent-requests', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('friendRequests')
                .where('fromUserId', '==', userId)
                .where('status', '==', 'PENDING')
                .get();
            const list = snapshot.docs.map(doc => {
                const data = doc.data();
                if (data.timestamp && typeof data.timestamp.toDate === 'function') {
                    data.timestamp = data.timestamp.toDate().toISOString();
                }
                return { id: doc.id, ...data };
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getSentRequests error:", e);
        }
    }

    const filtered = inMemoryDb.friendRequests.filter(r => r.fromUserId === userId && r.status === "PENDING");
    res.json(filtered);
});

app.post('/api/social/friend-requests/:id/accept', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            const rDocRef = db.collection('friendRequests').doc(id);
            const rDoc = await rDocRef.get();
            if (rDoc.exists) {
                const r = rDoc.data();
                await rDocRef.update({ status: 'ACCEPTED' });
                await db.collection('friendships').add({
                    userIds: [r.fromUserId, r.toUserId],
                    status: "ACCEPTED",
                    createdAt: new Date()
                });
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore acceptFriendRequest error:", e);
        }
    }

    const r = inMemoryDb.friendRequests.find(req => req.id === id);
    if (r) {
        r.status = "ACCEPTED";
        inMemoryDb.friendships.push({
            id: "friendship_" + Math.random().toString(36).substr(2, 9),
            userIds: [r.fromUserId, r.toUserId],
            status: "ACCEPTED",
            createdAt: new Date().toISOString()
        });
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Request not found" });
    }
});

app.post('/api/social/friend-requests/:id/decline', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('friendRequests').doc(id).update({ status: 'REJECTED' });
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore declineFriendRequest error:", e);
        }
    }

    const r = inMemoryDb.friendRequests.find(req => req.id === id);
    if (r) {
        r.status = "REJECTED";
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Request not found" });
    }
});

app.delete('/api/social/friends/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('friendships').doc(id).delete();
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore removeFriend error:", e);
        }
    }

    const index = inMemoryDb.friendships.findIndex(f => f.id === id);
    if (index !== -1) {
        inMemoryDb.friendships.splice(index, 1);
    }
    res.sendStatus(200);
});

app.get('/api/social/feed', async (req, res) => {
    if (useFirebase) {
        try {
            const snapshot = await db.collection('journals').get();
            const list = [];
            snapshot.forEach(doc => {
                const data = doc.data();
                if (data.timestamp && typeof data.timestamp.toDate === 'function') {
                    data.timestamp = data.timestamp.toDate().toISOString();
                }
                list.push({ id: doc.id, ...data });
            });
            list.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
            return res.json(list);
        } catch (e) {
            console.error("Firestore getSocialFeed error:", e);
        }
    }

    res.json(inMemoryDb.journals);
});

app.post('/api/social/posts/:id/react', async (req, res) => {
    const id = req.params.id;
    const { userId, emoji } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('journals').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const data = doc.data();
                const reactions = data.reactions || {};
                reactions[userId] = emoji;
                await docRef.update({ reactions });
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore reactToPost error:", e);
        }
    }

    const journal = inMemoryDb.journals.find(j => j.id === id);
    if (journal) {
        if (!journal.reactions) journal.reactions = {};
        journal.reactions[userId] = emoji;
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Post not found" });
    }
});

// --- Squads ---
app.get('/api/squads', async (req, res) => {
    const { userId } = req.query;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('squads').get();
            const list = [];
            snapshot.forEach(doc => {
                const data = doc.data();
                const members = data.members || [];
                if (members.includes(userId)) {
                    if (data.createdAt && typeof data.createdAt.toDate === 'function') {
                        data.createdAt = data.createdAt.toDate().toISOString();
                    }
                    list.push({ id: doc.id, ...data });
                }
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getSquads error:", e);
        }
    }

    const filtered = inMemoryDb.squads.filter(s => s.members.includes(userId));
    res.json(filtered);
});

app.post('/api/squads', async (req, res) => {
    const squad = req.body;
    const id = squad.id || "squad_" + Math.random().toString(36).substr(2, 9);
    squad.id = id;
    squad.createdAt = squad.createdAt || new Date().toISOString();

    if (useFirebase) {
        try {
            const dbSquad = { ...squad };
            dbSquad.createdAt = new Date(squad.createdAt);
            await db.collection('squads').doc(id).set(dbSquad);
            return res.status(201).json(squad);
        } catch (e) {
            console.error("Firestore createSquad error:", e);
        }
    }

    inMemoryDb.squads.push(squad);
    res.status(201).json(squad);
});

app.post('/api/squads/:id/join', async (req, res) => {
    const id = req.params.id;
    const { userId } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('squads').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const members = doc.data().members || [];
                if (!members.includes(userId)) {
                    members.push(userId);
                    await docRef.update({ members });
                }
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore joinSquad error:", e);
        }
    }

    const squad = inMemoryDb.squads.find(s => s.id === id);
    if (squad) {
        if (!squad.members.includes(userId)) {
            squad.members.push(userId);
        }
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Squad not found" });
    }
});

app.post('/api/squads/:id/leave', async (req, res) => {
    const id = req.params.id;
    const { userId } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('squads').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const members = doc.data().members || [];
                const idx = members.indexOf(userId);
                if (idx !== -1) {
                    members.splice(idx, 1);
                    await docRef.update({ members });
                }
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore leaveSquad error:", e);
        }
    }

    const squad = inMemoryDb.squads.find(s => s.id === id);
    if (squad) {
        const index = squad.members.indexOf(userId);
        if (index !== -1) {
            squad.members.splice(index, 1);
        }
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Squad not found" });
    }
});

app.put('/api/squads/:id', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            await db.collection('squads').doc(id).update(req.body);
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore updateSquad error:", e);
        }
    }

    const squad = inMemoryDb.squads.find(s => s.id === id);
    if (squad) {
        const { name, description } = req.body;
        if (name) squad.name = name;
        if (description) squad.description = description;
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Squad not found" });
    }
});

app.post('/api/squads/:id/members/add', async (req, res) => {
    const id = req.params.id;
    const { userId } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('squads').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const members = doc.data().members || [];
                if (!members.includes(userId)) {
                    members.push(userId);
                    await docRef.update({ members });
                }
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore addMemberToSquad error:", e);
        }
    }

    const squad = inMemoryDb.squads.find(s => s.id === id);
    if (squad) {
        if (!squad.members.includes(userId)) {
            squad.members.push(userId);
        }
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Squad not found" });
    }
});

app.post('/api/squads/:id/members/remove', async (req, res) => {
    const id = req.params.id;
    const { userId } = req.body;

    if (useFirebase) {
        try {
            const docRef = db.collection('squads').doc(id);
            const doc = await docRef.get();
            if (doc.exists) {
                const members = doc.data().members || [];
                const idx = members.indexOf(userId);
                if (idx !== -1) {
                    members.splice(idx, 1);
                    await docRef.update({ members });
                }
                return res.sendStatus(200);
            } else {
                return res.sendStatus(404);
            }
        } catch (e) {
            console.error("Firestore removeMemberFromSquad error:", e);
        }
    }

    const squad = inMemoryDb.squads.find(s => s.id === id);
    if (squad) {
        const index = squad.members.indexOf(userId);
        if (index !== -1) {
            squad.members.splice(index, 1);
        }
        res.sendStatus(200);
    } else {
        res.status(404).json({ error: "Squad not found" });
    }
});

app.get('/api/squads/:id/messages', async (req, res) => {
    const id = req.params.id;

    if (useFirebase) {
        try {
            const snapshot = await db.collection('squads').doc(id).collection('messages').orderBy('timestamp', 'asc').get();
            const list = snapshot.docs.map(doc => {
                const data = doc.data();
                if (data.timestamp && typeof data.timestamp.toDate === 'function') {
                    data.timestamp = data.timestamp.toDate().toISOString();
                }
                return { id: doc.id, ...data };
            });
            return res.json(list);
        } catch (e) {
            console.error("Firestore getSquadMessages error:", e);
        }
    }

    res.json(inMemoryDb.squadMessages);
});

app.post('/api/squads/:id/messages', async (req, res) => {
    const id = req.params.id;
    const { senderId, senderName, text } = req.body;

    if (useFirebase) {
        try {
            await db.collection('squads').doc(id).collection('messages').add({
                senderId,
                senderName,
                text,
                timestamp: new Date()
            });
            return res.sendStatus(200);
        } catch (e) {
            console.error("Firestore sendSquadMessage error:", e);
        }
    }

    const msg = {
        id: "msg_" + Math.random().toString(36).substr(2, 9),
        senderId,
        senderName,
        text,
        timestamp: new Date().toISOString()
    };
    inMemoryDb.squadMessages.push(msg);
    res.sendStatus(200);
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Mock server is running at http://localhost:${PORT}`);
    console.log(`Android Emulator should point to http://10.0.2.2:${PORT}`);
});
