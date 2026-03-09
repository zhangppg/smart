import Vue from 'vue'
import Router from 'vue-router'
import LoginView from '../views/LoginView.vue'
import PhotosView from '../views/PhotosView.vue'
import { getToken } from '../services/api'

Vue.use(Router)

const router = new Router({
  mode: 'hash',
  routes: [
    {
      path: '/',
      redirect: () => (getToken() ? '/photos' : '/login')
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/photos',
      name: 'photos',
      component: PhotosView
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authed = !!getToken()
  if (to.path === '/login' && authed) {
    next('/photos')
    return
  }
  if (to.path !== '/login' && !authed) {
    next('/login')
    return
  }
  next()
})

export default router
