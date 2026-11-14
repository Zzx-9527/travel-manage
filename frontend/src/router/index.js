import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/travel/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/travel/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', noAuth: true }
  },
  {
    // 用户端统一布局（首页 / 我的 等全部嵌套其下，避开与后台 /travel 子路由的路径冲突）
    path: '/travel/home',
    name: 'UserLayout',
    component: () => import('@/layouts/UserLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'UserHome',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'spots',
        name: 'SpotList',
        component: () => import('@/views/user/SpotList.vue'),
        meta: { title: '景点' }
      },
      {
        path: 'spots/:id',
        name: 'SpotDetail',
        component: () => import('@/views/user/SpotDetail.vue'),
        meta: { title: '景点详情' }
      },
      {
        path: 'guides',
        name: 'GuideList',
        component: () => import('@/views/user/GuideList.vue'),
        meta: { title: '攻略' }
      },
      {
        path: 'guides/:id',
        name: 'GuideDetail',
        component: () => import('@/views/user/GuideDetail.vue'),
        meta: { title: '攻略详情' }
      },
      {
        path: 'routes',
        name: 'RouteList',
        component: () => import('@/views/user/RouteList.vue'),
        meta: { title: '路线' }
      },
      {
        path: 'routes/:id',
        name: 'RouteDetail',
        component: () => import('@/views/user/RouteDetail.vue'),
        meta: { title: '路线详情' }
      },
      {
        path: 'planner',
        name: 'RoutePlanner',
        component: () => import('@/views/user/RoutePlanner.vue'),
        meta: { title: '路线规划' }
      },
      {
        // AI 行程规划：与上面的"手动选点 + 贪心排序"是两条独立链路，
        // 页面上互相引导，出问题时可降级到本地算法
        path: 'ai',
        name: 'AiPlanner',
        component: () => import('@/views/user/AiPlanner.vue'),
        meta: { title: 'AI 规划师' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/user/About.vue'),
        meta: { title: '关于平台' }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/user/Publish.vue'),
        meta: { title: '发布内容' }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/message/MessageCenter.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'mine',
        name: 'Mine',
        component: () => import('@/views/user/mine/MineLayout.vue'),
        redirect: '/travel/home/mine/profile',
        meta: { title: '个人中心' },
        children: [
          {
            path: 'profile',
            name: 'MyProfile',
            component: () => import('@/views/user/mine/MyProfile.vue'),
            meta: { title: '个人资料' }
          },
          {
            path: 'orders',
            name: 'MyOrders',
            component: () => import('@/views/user/mine/MyOrders.vue'),
            meta: { title: '我的订单' }
          },
          {
            path: 'favorites',
            name: 'MyFavorites',
            component: () => import('@/views/user/mine/MyFavorites.vue'),
            meta: { title: '我的收藏' }
          },
          {
            path: 'comments',
            name: 'MyComments',
            component: () => import('@/views/user/mine/MyComments.vue'),
            meta: { title: '我的评论' }
          },
          {
            path: 'publish',
            name: 'MyPublish',
            component: () => import('@/views/user/mine/MyPublish.vue'),
            meta: { title: '我的发布' }
          },
          {
            path: 'publish/itinerary',
            name: 'RouteItineraryEdit',
            component: () => import('@/views/user/RouteItineraryEdit.vue'),
            meta: { title: '配置路线行程' }
          }
        ]
      }
    ]
  },
  {
    path: '/travel',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/travel/dashboard',
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '系统概览' }
      },
      {
        path: 'ai-usage',
        name: 'AiUsage',
        component: () => import('@/views/AiUsage.vue'),
        meta: { title: 'AI 用量看板' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'categories',
        name: 'CategoryManagement',
        component: () => import('@/views/CategoryManagement.vue'),
        meta: { title: '景点分类管理' }
      },
      {
        path: 'spots',
        name: 'SpotManagement',
        component: () => import('@/views/ScenicManagement.vue'),
        meta: { title: '景点管理' }
      },
      {
        path: 'guides',
        name: 'GuideManagement',
        component: () => import('@/views/StrategyManagement.vue'),
        meta: { title: '旅游攻略管理' }
      },
      {
        path: 'routes',
        name: 'RouteManagement',
        component: () => import('@/views/RouteManagement.vue'),
        meta: { title: '旅游路线管理' }
      },
      {
        path: 'route-details',
        name: 'RouteDetailManagement',
        component: () => import('@/views/RouteDetailManagement.vue'),
        meta: { title: '路线途经景点管理' }
      },
      {
        path: 'comments',
        name: 'CommentManagement',
        component: () => import('@/views/CommentManagement.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'favorites',
        name: 'FavoriteManagement',
        component: () => import('@/views/CollectManagement.vue'),
        meta: { title: '收藏管理' }
      },
      {
        path: 'orders',
        name: 'OrderManagement',
        component: () => import('@/views/OrderManagement.vue'),
        meta: { title: '景点门票订单管理' }
      },
      {
        path: 'messages',
        name: 'AdminMessages',
        component: () => import('@/views/message/MessageCenter.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'announcements',
        name: 'MessageManagement',
        component: () => import('@/views/MessageManagement.vue'),
        meta: { title: '公告管理' }
      }
    ]
  },
  {
    // 404 兜底：未匹配到任何路由时不再白屏
    // 注意不加 noAuth：登录用户访问未知地址也应看到 404，而不是被弹回首页
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 导航守卫 — 读取 localStorage/sessionStorage，与 authStore 共享同一套键
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('travel_admin_token') || sessionStorage.getItem('travel_admin_token')
  let userInfo = null
  try {
    userInfo = JSON.parse(
      localStorage.getItem('travel_admin_user') || sessionStorage.getItem('travel_admin_user') || 'null'
    )
  } catch {
    userInfo = null
  }
  const isLoggedIn = !!token
  const isAdmin = userInfo?.userRole === 1

  // 已登录访问免登录页面（登录/注册）→ 按角色跳转
  if (to.matched.some(r => r.meta.noAuth) && isLoggedIn) {
    return next(isAdmin ? '/travel' : '/travel/home')
  }

  // 需要登录的页面
  if (to.matched.some(r => r.meta.requiresAuth) && !isLoggedIn) {
    return next('/travel/login')
  }

  // 需要管理员权限的页面
  if (to.matched.some(r => r.meta.requiresAdmin) && !isAdmin) {
    return next('/travel/home')
  }

  if (to.meta?.title) {
    document.title = `${to.meta.title} · 旅游信息管理平台`
  }

  next()
})

export default router
