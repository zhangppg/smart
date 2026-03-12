import Vue from 'vue'
import Router from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import PhotosView from '../views/PhotosView.vue'
import { getToken } from '../services/api'

Vue.use(Router)

const router = new Router({
  mode: 'hash',
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/photos',
      name: 'photos',
      component: PhotosView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authed = !!getToken()
  if (to.path === '/login' && authed) {
    next('/')
    return
  }
  if (to.matched.some(r => r.meta && r.meta.requiresAuth) && !authed) {
    next('/login')
    return
  }
  next()
})

export default router
