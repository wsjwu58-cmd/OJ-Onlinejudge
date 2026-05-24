import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'problems',
        name: 'Problems',
        component: () => import('../views/Problems.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'problems/create',
        name: 'CreateProblem',
        component: () => import('../views/CreateProblem.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'contests',
        name: 'Contests',
        component: () => import('../views/Contests.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('../views/Categories.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'submissions',
        name: 'Submissions',
        component: () => import('../views/Submissions.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'groups',
        name: 'Groups',
        component: () => import('../views/Groups.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/Statistics.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('../views/Knowledge.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const isLoggedIn = localStorage.getItem('token')
  
  if (requiresAuth && !isLoggedIn) {
    next({ name: 'Login' })
  } else if (to.path === '/login' && isLoggedIn) {
    // 如果已登录用户访问登录页，重定向到工作台
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router