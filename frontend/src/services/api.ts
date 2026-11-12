
import axios from 'axios'

const api = axios.create({ 
  baseURL: 'http://localhost:8082/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: true,
})

export default api
