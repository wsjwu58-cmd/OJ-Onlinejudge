import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, getUserProfile } from '../services/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: localStorage.getItem('token') || null,
    isAuthenticated: !!localStorage.getItem('token'),
    loading: false,
    error: null
  }),

  actions: {
    // 登录
    async login(username, password) {
      this.loading = true
      this.error = null
      try {
        const response = await apiLogin(username, password)
        this.token = response.token
        this.userInfo = response.user
        this.isAuthenticated = true
        localStorage.setItem('token', response.token)
        localStorage.setItem('user', JSON.stringify(response.user))
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    // 注册
    async register(username, email, password) {
      this.loading = true
      this.error = null
      try {
        const response = await apiRegister(username, email, password)
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取用户信息
    async fetchUserProfile() {
      if (!this.token) return
      
      this.loading = true
      this.error = null
      try {
        const user = await getUserProfile()
        this.userInfo = user
        localStorage.setItem('user', JSON.stringify(user))
        return user
      } catch (error) {
        this.error = error.message
        // 如果获取失败，可能是token过期，清除认证状态
        this.logout()
        throw error
      } finally {
        this.loading = false
      }
    },

    // 退出登录
    logout() {
      this.userInfo = null
      this.token = null
      this.isAuthenticated = false
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },

    // 从本地存储恢复用户信息
    restoreUser() {
      const storedUser = localStorage.getItem('user')
      if (storedUser) {
        this.userInfo = JSON.parse(storedUser)
      }
    }
  }
})