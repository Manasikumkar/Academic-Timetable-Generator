import axios from 'axios';

const API = axios.create({ baseURL: 'http://localhost:8081/api' });

// ─── JWT Interceptor – attach token to every request ─────────────────────
API.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Auth API ─────────────────────────────────────────────────────────────
export const authApi = {
  login:    (data) => API.post('/auth/login', data),
  register: (data) => API.post('/auth/register', null, { params: data })
};

// ─── Faculty API ──────────────────────────────────────────────────────────
export const facultyApi = {
  getAll:  ()          => API.get('/faculty'),
  create:  (data)      => API.post('/faculty', data),
  update:  (id, data)  => API.put(`/faculty/${id}`, data),
  delete:  (id)        => API.delete(`/faculty/${id}`),
};

// ─── Rooms API ────────────────────────────────────────────────────────────
export const roomApi = {
  getAll:  ()          => API.get('/rooms'),
  create:  (data)      => API.post('/rooms', data),
  update:  (id, data)  => API.put(`/rooms/${id}`, data),
  delete:  (id)        => API.delete(`/rooms/${id}`),
};

// ─── Courses API ──────────────────────────────────────────────────────────
export const courseApi = {
  getAll:      ()           => API.get('/courses'),
  getByYear:   (year)       => API.get(`/courses?yearClass=${year}`),
  create:      (data)       => API.post('/courses', data),
  update:      (id, data)   => API.put(`/courses/${id}`, data),
  delete:      (id)         => API.delete(`/courses/${id}`),
};

// ─── Constraints API ──────────────────────────────────────────────────────
export const constraintApi = {
  getAll:   ()    => API.get('/constraints'),
  create:   (data)=> API.post('/constraints', data),
  toggle:   (id)  => API.patch(`/constraints/${id}/toggle`),
  delete:   (id)  => API.delete(`/constraints/${id}`),
};

// ─── Timetable API ────────────────────────────────────────────────────────
export const timetableApi = {
  generate:     (data)          => API.post('/timetable/generate', data),
  getAllVersions:()              => API.get('/timetable/versions'),
  getVersion:   (id)            => API.get(`/timetable/versions/${id}`),
  getSlots:     (id, year)      => API.get(`/timetable/${id}/slots${year ? `?yearClass=${year}` : ''}`),
  getConflicts: (id)            => API.get(`/timetable/${id}/conflicts`),
  finalize:     (id)            => API.patch(`/timetable/${id}/finalize`),
  deploy:       (id)            => API.patch(`/timetable/${id}/deploy`),
  deleteVersion:(id)            => API.delete(`/timetable/${id}`),
  getActive:    (year)          => API.get(`/timetable/active${year ? `?yearClass=${year}` : ''}`),
  getCourses:   ()              => API.get("/courses"),
  getFaculties: ()              => API.get("/faculty"),
  getRooms:     ()              => API.get("/rooms"),
  // ✅ New methods for manual editing
  updateSlot:   (id, data)      => API.put(`/timetable/slot/${id}`, data),
  createSlot:   (data)          => API.post('/timetable/slot', data),
};

export default API;