import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../components/UserLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/HomePage.vue')
      },
      {
        path: 'problems',
        name: 'ProblemList',
        component: () => import('../views/ProblemList.vue')
      },
      {
        path: 'problems/:id',
        name: 'ProblemDetail',
        component: () => import('../views/ProblemDetailView.vue'),
        props: true
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'groups',
        name: 'GroupList',
        component: () => import('../views/GroupList.vue')
      },
      {
        path: 'groups/:id',
        name: 'GroupDetail',
        component: () => import('../views/GroupDetail.vue'),
        props: true
      },
      {
        path: 'contests',
        name: 'ContestList',
        component: () => import('../views/ContestList.vue')
      },
      {
        path: 'contests/:id',
        name: 'ContestDetail',
        component: () => import('../views/ContestDetail.vue'),
        props: true
      },
      {
        path: 'contests/:contestId/problems/:problemId',
        name: 'ContestProblem',
        component: () => import('../views/ContestProblemView.vue'),
        props: true,
        meta: { requiresAuth: true }
      },
      {
        path: 'submissions',
        name: 'SubmissionList',
        component: () => import('../views/SubmissionList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'solution/create/:problemId',
        name: 'SolutionCreate',
        component: () => import('../views/SolutionCreateView.vue'),
        meta: { requiresAuth: true },
        props: true
      },
      {
        path: 'solution/:id',
        name: 'SolutionDetail',
        component: () => import('../views/SolutionDetailView.vue'),
        props: true
      },
      {
        path: 'admin/knowledge',
        name: 'KnowledgeManage',
        component: () => import('../views/KnowledgeManage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
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
  } else if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
