import { useEffect, useRef, useState } from 'react'
import {
  ArrowLeft,
  ArrowUpRight,
  BookOpen,
  BrainCircuit,
  Check,
  ChevronRight,
  CircleUserRound,
  ExternalLink,
  GraduationCap,
  LibraryBig,
  LogIn,
  Mail,
  Plus,
  Search,
  Send,
  Settings2,
  Sparkles,
  Upload,
  UserRound,
  X,
} from 'lucide-react'
import { AnimatePresence, motion } from 'motion/react'
import authorAvatar from './assets/jinyu-avatar.jpg'

const easing = [0.16, 1, 0.3, 1]
const heroVideoUrl = '/media/hero-hand.mp4'

const routes = {
  '/': '首页',
  '/blog': '博客',
  '/learn': '知识树',
  '/questions': '个人题库',
  '/about': '关于我',
  '/profile': '个人中心',
  '/login': '登录',
  '/register': '注册',
}

const articles = [
  {
    title: '从“知道”到“会用”：把学习变成一条可回看的路径',
    excerpt: '知识不是不断堆积的笔记。把一个问题拆开、连接、再回到自己的语言里，理解才会真正留下来。',
    category: '学习方法',
    date: '2026.08.09',
    read: '6 分钟阅读',
    image: 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1200&q=85',
    alt: '电路板细节',
  },
  {
    title: 'JVM 垃圾回收：从一张知识树开始理解',
    excerpt: '从对象为什么会消失，到不同收集器如何取舍。用分层问题代替零散结论，才能在需要时讲清楚。',
    category: 'Java',
    date: '2026.08.04',
    read: '12 分钟阅读',
    image: 'https://images.unsplash.com/photo-1501504905252-473c47e087f8?auto=format&fit=crop&w=1200&q=85',
    alt: '桌面上的学习笔记',
  },
  {
    title: '题库不是收藏夹：建立自己的复习闭环',
    excerpt: '保留题目的来源、答案和错题记录，再让每一次练习形成可用的反馈，而不是完成一次随机测验。',
    category: '个人系统',
    date: '2026.07.28',
    read: '8 分钟阅读',
    image: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=85',
    alt: '电脑前的工作台',
  },
]

const originalTreeQuestion = '请解释 Redis 的单线程模型如何做到高效与线程安全。'

const treeNodes = [
  { id: 'root', parentId: null, title: 'Redis 单线程模型：如何做到高效与线程安全', level: 0, summary: 'Redis 的命令执行核心以单线程串行处理为主，但高性能来自事件驱动、I/O 多路复用和高效数据结构，而不是依赖多个执行线程抢占 CPU。', sections: [{ title: '核心结论', text: 'Redis 并非所有工作都只有一个线程；它的命令执行路径主要是单线程串行执行。网络事件由 I/O 多路复用统一调度，耗时任务则可以交给后台线程或子进程，因此同时获得清晰的并发语义与很高的吞吐。' }] },
  { id: 'single-thread', parentId: 'root', title: 'Redis 单线程指什么', level: 1, summary: '单线程指核心命令执行串行化，不等于 Redis 进程里永远只有一个线程。', sections: [{ title: '事件驱动模型', text: 'Redis 使用事件循环处理网络事件。主线程监听客户端 Socket 连接，读取请求命令，顺序执行 set、get、incr 等内存操作，再将结果写回客户端。' }, { title: '串行执行的结果', text: '同一时刻只有一条命令修改数据，因此不需要为核心命令路径引入复杂的锁竞争。命令执行完成后，事件循环继续处理下一个就绪事件。' }], answer: 'Redis 的单线程主要约束命令执行路径：命令在事件循环中顺序执行。你可以继续追问事件循环如何调度，或单线程和后台线程分别承担什么工作。' },
  { id: 'why-performance', parentId: 'root', title: '为什么单线程还能高性能', level: 1, summary: '性能瓶颈常在网络和内存访问；Redis 用事件循环同时管理大量连接，并让主线程专注于短小的内存操作。', sections: [{ title: '先看性能前提', text: 'Redis 的数据主要驻留内存，单条命令通常很短。它要解决的重点不是让多个线程同时计算，而是高效等待网络事件、快速执行内存读写并减少调度成本。' }] },
  { id: 'cpu-not-bottleneck', parentId: 'why-performance', title: '瓶颈不在 CPU，而在内存与网络', level: 2, summary: '内存访问速度快，很多 Redis 命令不是 CPU 密集型计算。', sections: [{ title: '单线程的适用条件', text: 'Redis 的核心操作直接面向内存数据结构，速度很快。对常见命令而言，网络 I/O、内存带宽和命令本身的复杂度更值得关注；单线程反而省去了线程切换和锁竞争。' }], answer: '单线程高性能的前提是命令足够短、数据在内存中、CPU 不是主要瓶颈。若出现大 key 或高复杂度命令，单线程同样会被阻塞。' },
  { id: 'io-multiplexing', parentId: 'why-performance', title: '基于网络 I/O 多路复用', level: 2, summary: '一个线程可以同时监听大量客户端连接，只在连接就绪时处理它。', sections: [{ title: '事件如何被发现', text: 'Redis 在不同系统上使用 epoll、kqueue 或 io_uring 等机制。事件循环不会为每个连接创建并阻塞一个线程，而是等待内核返回已经可读、可写的连接集合。' }, { title: '为什么不空转', text: '没有连接就绪时，线程阻塞在事件等待上，不消耗 CPU；有客户端发来命令时，操作系统唤醒主线程，Redis 依次处理就绪事件。' }], answer: 'I/O 多路复用让一个事件循环管理大量连接。可以继续追问 epoll 的就绪通知、事件循环的处理顺序，或大量慢客户端会带来什么影响。' },
  { id: 'avoid-concurrency', parentId: 'why-performance', title: '避免多线程调度与锁竞争', level: 2, summary: '少一个共享状态并发执行层，就少线程创建、上下文切换和互斥成本。', sections: [{ title: '多线程的隐性成本', text: '多线程需要考虑线程创建、上下文切换、共享状态同步和 CPU 缓存失效。Redis 把核心命令串行化后，数据访问路径更直接，也避免了围绕每个键设计细粒度锁。' }], answer: 'Redis 不是因为线程越少越好，而是在短命令和内存数据的场景下，用串行执行换取更小的调度与同步成本。' },
  { id: 'efficient-structures', parentId: 'why-performance', title: '高效内存结构与少量系统调用', level: 2, summary: '数据结构和协议处理同样决定吞吐，单线程只是整体设计的一部分。', sections: [{ title: '少做无效工作', text: 'Redis 使用紧凑高效的数据结构，并通过批量命令、pipeline 和协议解析降低网络往返与系统调用次数。吞吐提升来自让每次事件处理携带更多有效工作。' }], answer: '高效数据结构减少单条命令成本，pipeline 减少网络往返。两者和单线程事件循环共同构成 Redis 的高吞吐路径。' },
  { id: 'background-work', parentId: 'why-performance', title: '耗时操作不阻塞主线程', level: 2, summary: '主线程不承担所有慢任务，后台线程和子进程会分担特定工作。', sections: [{ title: '主线程之外的工作', text: '大 key 删除可使用 UNLINK 异步释放内存；RDB 或 AOF 相关工作可借助 fork 子进程；文件同步、惰性释放等任务也可以交由后台执行。需要注意，fork 期间仍会有内存与写时复制成本。' }], answer: 'Redis 的“单线程”不意味着没有辅助执行单元。关键是核心命令执行路径保持串行，耗时工作被有意识地移出主线程。' },
  { id: 'safety', parentId: 'root', title: '单线程如何保证安全', level: 1, summary: '串行命令执行避免了核心数据的并发写入冲突，单条命令通常具备原子性。', sections: [{ title: '安全边界', text: '线程安全不是“完全没有并发”，而是对共享数据的修改有明确顺序。Redis 让进入事件循环的命令逐条完成，再开始下一条。' }] },
  { id: 'serial-execution', parentId: 'safety', title: '所有命令串行执行', level: 2, summary: '同一时刻只有一条命令在核心数据路径上执行。', sections: [{ title: '顺序带来的确定性', text: '多个客户端命令进入队列后，主线程按事件循环选定的顺序处理。因为没有两条命令同时修改同一份数据，核心命令无需额外加锁，也不会出现典型的竞态写入。' }], answer: '串行执行保证的是核心命令修改数据时不存在并发交叉。可以继续追问多个客户端同时写同一个键时的顺序，或阻塞命令如何影响事件循环。' },
  { id: 'command-atomicity', parentId: 'safety', title: '单条命令的原子性', level: 2, summary: '一条 Redis 命令执行过程中不会被另一条命令插入。', sections: [{ title: '原子性意味着什么', text: '例如 setnx、hincrby 等命令在执行完成前不会被其他命令打断。多条命令组合并不天然原子；需要事务、Lua 脚本或 Redis Function 时，应明确它们的执行边界和阻塞风险。' }], answer: '单条命令原子不等于多条命令原子。涉及“先判断再修改”的多步逻辑时，需要根据场景使用事务、Lua 或 Function。' },
  { id: 'misconceptions', parentId: 'root', title: '常见误区澄清', level: 1, summary: '理解 Redis 单线程时，要区分命令执行模型、网络处理能力与后台任务。', sections: [{ title: '先区分概念', text: '“单线程”通常指核心命令执行路径，而不是 Redis 内部不存在其他线程或进程。脱离这个边界讨论性能和安全，容易得到错误结论。' }] },
  { id: 'one-thread-myth', parentId: 'misconceptions', title: 'Redis 整个程序只有一个线程', level: 2, summary: '这是错误说法，核心命令执行单线程不代表没有 I/O 或后台辅助线程。', sections: [{ title: '正确理解', text: 'Redis 6 以后可将部分网络 I/O 交给额外线程，持久化和惰性释放也有后台工作；但数据命令执行仍保持串行化，以维持简单可靠的共享数据语义。' }], answer: '应该把“单线程”限定为核心命令执行模型。网络 I/O 与后台任务可以并行，但不能把它们和命令执行混为一谈。' },
  { id: 'multi-core-myth', parentId: 'misconceptions', title: '单线程无法利用多核 CPU', level: 2, summary: '单个 Redis 实例的命令执行主线程受限于一个核心，但整体部署可以使用多个核心。', sections: [{ title: '扩展方式', text: '可以部署多个 Redis 实例，让它们绑定不同 CPU 核心；也可以使用主从、哨兵或集群把数据和流量分摊。Redis 6 的 I/O 多线程也能分担读写网络处理。' }], answer: '单实例命令执行主要使用一个核心，但整机多核仍可通过多实例、集群与 I/O 线程获得利用。' },
  { id: 'no-blocking-myth', parentId: 'misconceptions', title: '单线程不会发生阻塞', level: 2, summary: '这是错误说法，大 key、慢命令和同步持久化都可能拉长事件循环。', sections: [{ title: '需要避免的情况', text: '大 key 删除、keys、全量扫描方式不当、耗时 Lua 脚本以及某些持久化阶段，都可能让主线程响应变慢。单线程模型更要求命令短小、避免长时间占用事件循环。' }], answer: '单线程不会自动避免阻塞。排查延迟时需要关注慢日志、大 key、命令复杂度和持久化期间的资源消耗。' },
  { id: 'one-sentence-summary', parentId: 'root', title: '总结一句话', level: 1, summary: 'Redis 通过串行命令、事件驱动与后台分工，在高吞吐和简单安全的并发语义之间取得平衡。', sections: [{ title: '最终结论', text: 'Redis 的多路复用负责管理大量连接，命令执行通过单线程串行完成；少量耗时任务被移出主线程。这样既减少线程切换和锁竞争，也让核心数据路径保持可预测与安全。' }], answer: 'Redis 的关键不是“只有一个线程”，而是让核心命令串行执行、让事件循环高效调度连接、让慢任务离开主路径。' },
]

const nodeHistories = {
  'single-thread': [{ role: 'assistant', text: 'Redis 的单线程主要指核心命令执行在事件循环中串行完成；网络事件和后台任务需要与这条主路径区分开看。' }],
  'io-multiplexing': [{ role: 'assistant', text: 'I/O 多路复用让事件循环在大量连接中只处理已经就绪的连接，因此不必为每个客户端创建并阻塞一个线程。' }],
  'command-atomicity': [{ role: 'assistant', text: '单条命令执行期间不会被其他命令插入；多条命令的组合逻辑则要使用事务、Lua 或 Function 明确原子边界。' }],
  'no-blocking-myth': [{ role: 'assistant', text: '单线程并不等于不会阻塞。大 key、慢命令和长时间脚本都会占住事件循环，直接影响其他客户端的响应时间。' }],
}

const treeNodeById = Object.fromEntries(treeNodes.map((node) => [node.id, node]))

function getTreeLineage(node) {
  const lineage = []
  let current = node
  while (current) {
    lineage.unshift(current)
    current = current.parentId ? treeNodeById[current.parentId] : null
  }
  return lineage
}

function getTreeChildren(nodeId) {
  return treeNodes.filter((node) => node.parentId === nodeId)
}

function getSubtreeNodes(node) {
  return [node, ...getTreeChildren(node.id).flatMap((child) => getSubtreeNodes(child))]
}

function isDescendantOf(node, ancestorId) {
  let current = node
  while (current.parentId) {
    if (current.parentId === ancestorId) return true
    current = treeNodeById[current.parentId]
  }
  return false
}

function getVisibleTreeNodes(expandedNodeIds) {
  const visibleNodes = []
  const visit = (node) => {
    visibleNodes.push(node)
    if (expandedNodeIds.has(node.id)) getTreeChildren(node.id).forEach(visit)
  }
  visit(treeNodes[0])
  return visibleNodes
}

const librarySeed = [
  { id: 'java', name: 'Java 核心', count: 86 },
  { id: 'network', name: '计算机网络', count: 42 },
  { id: 'wrong', name: '错题知识库', count: 17, system: true },
]

function BrandMark() {
  return (
    <svg className="brand-mark" viewBox="0 0 30 30" aria-hidden="true">
      <rect x="5" y="4" width="11" height="23" rx="5.5" transform="rotate(-35 10.5 15.5)" />
      <rect x="14" y="3" width="11" height="23" rx="5.5" transform="rotate(-35 19.5 14.5)" />
    </svg>
  )
}

function GridIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      {[3.25, 8.75].flatMap((cy) => [3.25, 8.75].map((cx) => <circle key={`${cx}-${cy}`} cx={cx} cy={cy} r="1.45" />))}
    </svg>
  )
}

function useRoute() {
  const [route, setRoute] = useState(() => (routes[window.location.pathname] ? window.location.pathname : '/'))

  useEffect(() => {
    const handlePopstate = () => setRoute(routes[window.location.pathname] ? window.location.pathname : '/')
    window.addEventListener('popstate', handlePopstate)
    return () => window.removeEventListener('popstate', handlePopstate)
  }, [])

  const navigate = (target) => {
    window.history.pushState({}, '', target)
    setRoute(target)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return [route, navigate]
}

function App() {
  const [route, navigate] = useRoute()

  return (
    <>
      <PersistentHeroVideo active={route === '/'} />
      <AnimatePresence mode="wait">
        <motion.div key={route} className="route-transition" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} transition={{ duration: 0.5, ease: easing }}>
          {route === '/' && <LandingPage navigate={navigate} />}
          {route === '/blog' && <BlogPage navigate={navigate} />}
          {route === '/learn' && <LearnPage navigate={navigate} />}
          {route === '/questions' && <QuestionPage navigate={navigate} />}
          {route === '/about' && <AboutPage navigate={navigate} />}
          {route === '/profile' && <ProfilePage navigate={navigate} />}
          {(route === '/login' || route === '/register') && <AuthPage mode={route === '/login' ? 'login' : 'register'} navigate={navigate} />}
        </motion.div>
      </AnimatePresence>
    </>
  )
}

function PersistentHeroVideo({ active }) {
  const videoRef = useRef(null)

  const freezeOnFinalFrame = (event) => {
    const video = event.currentTarget
    video.currentTime = Math.max(0, video.duration - 0.08)
    video.pause()
  }

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    if (active) {
      video.play().catch(() => undefined)
      return
    }

    video.pause()
  }, [active])

  return (
    <motion.div className="persistent-video-motion" initial={{ opacity: 0, scale: 1.05 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 1.8, ease: easing }}>
      <div className={`persistent-video-stage ${active ? 'is-active' : ''}`} aria-hidden={!active}>
        <video ref={videoRef} className="background-video" autoPlay muted playsInline preload="auto" onEnded={freezeOnFinalFrame}>
          <source src={heroVideoUrl} type="video/mp4" />
        </video>
        <div className="video-wash" aria-hidden="true" />
      </div>
    </motion.div>
  )
}

function TopNav({ navigate, current }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuItems = [
    { route: '/blog', label: '阅读博客', detail: '文章、标签与收藏', icon: BookOpen },
    { route: '/learn', label: 'AI 知识树', detail: '提出问题，继续追问', icon: BrainCircuit },
    { route: '/questions', label: '个人题库', detail: '导入、审核与刷题', icon: LibraryBig },
    { route: '/profile', label: '个人中心', detail: '资料与学习偏好', icon: UserRound },
  ]

  const go = (target) => {
    setMenuOpen(false)
    navigate(target)
  }

  return (
    <>
      <nav className={`site-nav ${current === '/' ? 'site-nav-home' : 'site-nav-app'}`} aria-label="主导航">
        <div className="nav-left">
          <button className="brand" type="button" onClick={() => go('/')} aria-label="返回狠狠学首页"><BrandMark /><span className="brand-name">狠狠学</span></button>
          <button className="menu-trigger" type="button" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen} aria-controls="site-menu"><span className="menu-plus"><Plus size={12} strokeWidth={3} /></span><span>菜单</span></button>
          <div className="nav-tags" aria-label="产品模块">
            <button type="button" onClick={() => go('/blog')}>博客</button>
            <button type="button" onClick={() => go('/learn')}>AI 知识树</button>
            <button type="button" onClick={() => go('/questions')}>个人题库</button>
          </div>
        </div>
        <div className="nav-right">
          {current !== '/' && <button className="nav-login" type="button" onClick={() => go('/login')}><LogIn size={14} /><span>登录</span></button>}
          <button className="system-trigger" type="button" onClick={() => go('/profile')} aria-label="打开个人中心"><span className="grid-button">{current === '/' ? <GridIcon /> : <CircleUserRound size={15} />}</span><span className="system-label">{current === '/' ? '学习中枢' : '我的空间'}</span></button>
        </div>
      </nav>
      <AnimatePresence>
        {menuOpen && (
          <motion.aside id="site-menu" className="menu-panel" initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} transition={{ duration: 0.35, ease: easing }} aria-label="功能导航">
            <div className="panel-heading"><span>探索狠狠学</span><button type="button" onClick={() => setMenuOpen(false)} aria-label="关闭菜单"><X size={16} /></button></div>
            {menuItems.map(({ route, label, detail, icon: Icon }) => <button key={route} type="button" className="menu-item" onClick={() => go(route)}><span className="menu-item-icon"><Icon size={17} strokeWidth={1.8} /></span><span><strong>{label}</strong><small>{detail}</small></span><ArrowUpRight size={16} strokeWidth={1.8} /></button>)}
            <div className="menu-auth"><button type="button" onClick={() => go('/login')}>登录</button><button type="button" onClick={() => go('/register')}>注册账号</button></div>
          </motion.aside>
        )}
      </AnimatePresence>
    </>
  )
}

function LandingPage({ navigate }) {
  return (
    <main className="landing-shell">
      <section className="landing-hero" aria-labelledby="hero-title">
        <TopNav navigate={navigate} current="/" />
        <motion.footer className="hero-footer" initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ delay: 0.5, duration: 1, ease: easing }}>
          <section className="hero-copy">
            <motion.p className="hero-subtitle" initial={{ y: 16, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ delay: 0.6, duration: 0.8, ease: easing }}><span className="subtitle-dot" aria-hidden="true" />你的私人 AI 学习空间</motion.p>
            <motion.h1 id="hero-title" initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ delay: 0.8, duration: 0.8, ease: easing }}>把知识学透<br />再练成能力</motion.h1>
            <motion.div className="hero-actions" initial={{ y: 16, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ delay: 1, duration: 0.8, ease: easing }}><button type="button" className="primary-action" onClick={() => navigate('/learn')}>开始学习 <ArrowUpRight size={15} strokeWidth={2} /></button><button type="button" className="secondary-action" onClick={() => navigate('/blog')}>阅读博客</button></motion.div>
          </section>
          <motion.div className="topic-tags" initial={{ y: 16, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ delay: 1.05, duration: 0.8, ease: easing }} aria-label="核心主题">{['深度学习', '知识树', '个人题库'].map((tag) => <button type="button" key={tag} onClick={() => navigate(tag === '个人题库' ? '/questions' : '/learn')}>{tag}</button>)}</motion.div>
        </motion.footer>
      </section>
      <section className="home-about-preview" aria-labelledby="home-about-title">
        <div className="home-about-person"><span className="home-about-avatar" aria-hidden="true"><img src={authorAvatar} alt="" /></span><div><strong>瑾瑜</strong><p>烟台科技学院<br />28届计算机专业本科生</p></div></div>
        <div className="home-about-copy"><p className="section-label">关于作者</p><h2 id="home-about-title">把正在学习的路，<br />走成能分享的东西。</h2><p>烟台科技学院28届计算机专业本科生；正在学习 Java 全栈与 AI 应用开发。这个博客记录文章、心得与想法，也尝试提供一条 AI 辅助学习的路径。</p><button type="button" className="text-action" onClick={() => navigate('/about')}>认识瑾瑜 <ArrowUpRight size={15} /></button></div>
      </section>
    </main>
  )
}

function PageIntro({ title, summary, label, children }) {
  return <header className="page-intro"><div><p className="section-label">{label}</p><h1>{title}</h1><p className="page-summary">{summary}</p></div>{children}</header>
}

function AppPage({ navigate, current, children }) {
  return <main className="app-shell"><TopNav navigate={navigate} current={current} /><div className="app-page">{children}</div></main>
}

function BlogPage({ navigate }) {
  const [activeTag, setActiveTag] = useState('全部文章')
  const filteredArticles = activeTag === '全部文章' ? articles : articles.filter((article) => article.category === activeTag)
  const tags = ['全部文章', '学习方法', 'Java', '个人系统']

  return (
    <AppPage navigate={navigate} current="/blog">
      <PageIntro label="博客" title={<>把每一次学懂<br />写成可回看的文章</>} summary="管理员发布的学习文章，按主题整理。收藏、目录与阅读进度会在真实服务接入后保留到你的账户。"><button className="page-round-action" type="button" aria-label="搜索文章"><Search size={17} /></button></PageIntro>
      <div className="tag-filter" aria-label="博客分类">{tags.map((tag) => <button className={activeTag === tag ? 'is-active' : ''} key={tag} type="button" onClick={() => setActiveTag(tag)}>{tag}</button>)}</div>
      <section className="article-feed" aria-label="文章列表">
        {filteredArticles.map((article, index) => <motion.article className="article-strip" key={article.title} initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55, delay: index * 0.07, ease: easing }}>
          <div className="article-image"><img src={article.image} alt={article.alt} /></div>
          <div className="article-copy"><div className="article-meta"><span>{article.category}</span><span>{article.date}</span><span>{article.read}</span></div><h2>{article.title}</h2><p>{article.excerpt}</p><button type="button" className="text-action" onClick={() => navigate('/login')}>阅读文章 <ArrowUpRight size={15} /></button></div>
        </motion.article>)}
      </section>
    </AppPage>
  )
}

function LearnPage({ navigate }) {
  const [selectedNode, setSelectedNode] = useState(treeNodes[0])
  const [question, setQuestion] = useState('')
  const [historyByNode, setHistoryByNode] = useState(nodeHistories)
  const lineage = getTreeLineage(selectedNode)
  const [expandedNodeIds, setExpandedNodeIds] = useState(() => new Set(['root']))
  const directChildren = getTreeChildren(selectedNode.id)
  const siblingNodes = selectedNode.parentId ? getTreeChildren(selectedNode.parentId) : []
  const isLeafNode = directChildren.length === 0
  const displayedNodes = directChildren.length ? directChildren : siblingNodes
  const selectedHistory = isLeafNode ? historyByNode[selectedNode.id] || [] : []
  const subtreeNodes = getSubtreeNodes(selectedNode)
  const visibleTreeNodes = getVisibleTreeNodes(expandedNodeIds)

  const selectNode = (node) => {
    setSelectedNode(node)
    setQuestion('')
    setExpandedNodeIds((expanded) => {
      const next = new Set(expanded)
      getTreeLineage(node).slice(0, -1).forEach((ancestor) => next.add(ancestor.id))
      return next
    })
  }

  const toggleBranch = (node) => {
    const willCollapse = expandedNodeIds.has(node.id)
    setExpandedNodeIds((expanded) => {
      const next = new Set(expanded)
      if (willCollapse) next.delete(node.id)
      else next.add(node.id)
      return next
    })
    if (willCollapse && isDescendantOf(selectedNode, node.id)) {
      setSelectedNode(node)
      setQuestion('')
    }
  }

  const ask = (event) => {
    event.preventDefault()
    if (!isLeafNode) return
    const trimmed = question.trim()
    if (!trimmed) return
    setHistoryByNode((histories) => ({
      ...histories,
      [selectedNode.id]: [...(histories[selectedNode.id] || []), { role: 'user', text: trimmed }, { role: 'assistant', text: selectedNode.answer }],
    }))
    setQuestion('')
  }

  return (
    <AppPage navigate={navigate} current="/learn">
      <PageIntro label="AI 知识树" title={<>从一个问题<br />长出自己的理解</>} summary="标题会解析为树节点。父节点用于浏览它辖下的完整内容，只有没有子标题的叶子节点可以继续追问。"><button type="button" className="primary-action compact-action" onClick={() => selectNode(treeNodes[0])}><Plus size={15} />新建知识树</button></PageIntro>
      <section className="learn-workspace" aria-label="知识树工作台">
        <aside className="tree-sidebar"><div className="pane-head"><span>我的知识树</span><button type="button" aria-label="树设置"><Settings2 size={15} /></button></div><button type="button" className="tree-query"><Search size={14} />搜索节点</button><ol className="tree-list">{visibleTreeNodes.map((node) => { const hasChildren = getTreeChildren(node.id).length > 0; const isExpanded = expandedNodeIds.has(node.id); return <li key={node.id} style={{ '--node-level': node.level }}>{hasChildren ? <button type="button" className="tree-node-toggle" aria-label={`${isExpanded ? '收起' : '展开'}「${node.title}」`} aria-expanded={isExpanded} onClick={() => toggleBranch(node)}><ChevronRight className={isExpanded ? 'is-expanded' : ''} size={14} /></button> : <span className="tree-node-spacer" />}<button type="button" className={`tree-node-button ${selectedNode.id === node.id ? 'is-selected' : ''}`} onClick={() => selectNode(node)}>{node.title}</button></li>})}</ol><div className="tree-version"><span>当前版本</span><strong>V3 · 3 分钟前</strong></div></aside>
        <section className="tree-stage"><div className="stage-caption"><div className="tree-path"><span>当前路径</span><strong>{lineage.map((node) => node.title).join(' / ')}</strong></div><button type="button" onClick={() => selectNode(treeNodes[0])}>回到根节点</button></div><div className="tree-origin"><span>原始提问</span><p>{originalTreeQuestion}</p></div><div className={`tree-map ${isLeafNode ? 'is-leaf-view' : ''}`} aria-label="Redis 单线程模型知识树"><div className="tree-map-lineage">{lineage.slice(0, -1).map((node) => <button key={node.id} type="button" className="map-lineage-node" onClick={() => selectNode(node)}>{node.title}</button>)}</div>{isLeafNode ? <div className="tree-map-level" aria-label="当前层级节点">{displayedNodes.map((node) => <button key={node.id} type="button" className={`map-node ${selectedNode.id === node.id ? 'is-selected' : ''}`} onClick={() => selectNode(node)}>{node.title}</button>)}</div> : <><button type="button" className="map-node is-selected" onClick={() => selectNode(selectedNode)}>{selectedNode.title}</button>{displayedNodes.length > 0 && <div className="tree-map-next"><span>展开直接子节点</span><div>{displayedNodes.map((node) => <button key={node.id} type="button" className="map-node" onClick={() => selectNode(node)}>{node.title}</button>)}</div></div>}</>}</div><article className="node-detail"><div className="node-detail-head"><span>{isLeafNode ? '叶子节点内容' : '节点包含的完整内容'}</span><small>{isLeafNode ? `历史追问 ${selectedHistory.length} 条` : `包含 ${subtreeNodes.length - 1} 个后代节点`}</small></div><h2>{selectedNode.title}</h2><p className="node-summary">{selectedNode.summary}</p>{!isLeafNode && <p className="node-browse-note">这是结构节点，正文已汇总展示；请选择没有子标题的最末级节点后再追问。</p>}<div className="node-subtree">{subtreeNodes.map((node, index) => <section className="node-subtree-section" style={{ '--content-depth': node.level - selectedNode.level }} key={node.id}>{index > 0 && <button type="button" className="node-content-title" onClick={() => selectNode(node)}>{node.title}<ChevronRight size={14} /></button>}{node.sections.map((section) => <div className="node-section-copy" key={section.title}><h3>{section.title}</h3><p>{section.text}</p></div>)}</section>)}</div></article></section>
        <aside className={`conversation-pane ${isLeafNode ? '' : 'is-locked'}`}>{isLeafNode ? <><div className="pane-head"><span>{selectedNode.title} · 历史追问</span><span className="live-dot">上下文已就绪</span></div><p className="conversation-scope">追问只补充当前叶子节点，不重新生成知识树。</p><div className="conversation-list">{selectedHistory.map((message, index) => <div key={`${selectedNode.id}-${message.role}-${index}`} className={`message message-${message.role}`}><span>{message.role === 'assistant' ? 'AI' : '你'}</span><p>{message.text}</p></div>)}</div><form className="ask-form" onSubmit={ask}><label htmlFor="follow-up">继续追问「{selectedNode.title}」</label><div><input id="follow-up" value={question} onChange={(event) => setQuestion(event.target.value)} placeholder={`例如：${selectedNode.title}在什么场景下会成为瓶颈？`} /><button type="submit" aria-label="发送问题"><Send size={15} /></button></div></form></> : <><div className="pane-head"><span>{selectedNode.title}</span><span className="locked-state">仅浏览</span></div><div className="conversation-locked"><h2>这是结构节点。</h2><p>它包含 {directChildren.length} 个直接子节点。当前可浏览完整内容；进入没有子标题的最末级节点后，才可以向 AI 追问。</p><div>{directChildren.map((node) => <button key={node.id} type="button" onClick={() => selectNode(node)}>{node.title}<ChevronRight size={15} /></button>)}</div></div></>}</aside>
      </section>
    </AppPage>
  )
}

function QuestionPage({ navigate }) {
  const [activeLibrary, setActiveLibrary] = useState('java')
  const [mode, setMode] = useState('exam')
  const [uploadName, setUploadName] = useState('')
  const [practiceStarted, setPracticeStarted] = useState(false)
  const [answer, setAnswer] = useState('')
  const [result, setResult] = useState(null)
  const [questionCount, setQuestionCount] = useState(15)
  const activeLibraryName = librarySeed.find((library) => library.id === activeLibrary)?.name

  const submitAnswer = () => setResult(answer === 'B' ? 'correct' : 'wrong')

  return (
    <AppPage navigate={navigate} current="/questions">
      <PageIntro label="个人题库" title={<>把自己的材料<br />练成能调用的答案</>} summary="题库默认私有。这里模拟从 MinerU 上传 Markdown ZIP、手动录题、选择范围与开始刷题的完整入口。"><button type="button" className="primary-action compact-action" onClick={() => setPracticeStarted(false)}><Plus size={15} />新建题库</button></PageIntro>
      <section className="question-workspace" aria-label="个人题库工作台">
        <aside className="library-pane"><div className="pane-head"><span>我的题库</span><button type="button" aria-label="新建题库"><Plus size={15} /></button></div><div className="library-list">{librarySeed.map((library) => <button key={library.id} className={activeLibrary === library.id ? 'is-selected' : ''} type="button" onClick={() => setActiveLibrary(library.id)}><span><LibraryBig size={15} />{library.name}</span><small>{library.count}</small></button>)}</div><div className="import-block"><div><span className="section-label">导入题目</span><h2>从文档开始</h2><p>先在 MinerU 完成文档转换，再上传一个不超过 30 MiB 的 ZIP。</p></div><a href="https://mineru.net/" target="_blank" rel="noreferrer">前往 MinerU <ExternalLink size={14} /></a><label className="upload-control"><Upload size={15} /><span>{uploadName || '上传 MinerU ZIP'}</span><input type="file" accept=".zip" onChange={(event) => setUploadName(event.target.files?.[0]?.name || '')} /></label><button type="button" className="manual-entry" onClick={() => setUploadName('手动题目草稿已创建')}>手动输入题目 <ChevronRight size={15} /></button></div></aside>
        <section className="practice-pane">
          {!practiceStarted ? <div className="practice-setup"><div className="pane-head"><span>开始刷题</span><span className="setup-state">范围会在开始时冻结</span></div><div className="setup-lead"><span className="large-index">{String(questionCount).padStart(2, '0')}</span><div><h2>从 {activeLibraryName} 开始。</h2><p>选择题库、模式与数量；这次练习会保留独立的题目快照。</p></div></div><div className="setup-row"><span>练习模式</span><div className="segment-control"><button className={mode === 'memorize' ? 'is-active' : ''} type="button" onClick={() => setMode('memorize')}>背题模式</button><button className={mode === 'exam' ? 'is-active' : ''} type="button" onClick={() => setMode('exam')}>作答模式</button></div></div><div className="setup-row"><span>题目数量</span><div className="stepper"><button type="button" onClick={() => setQuestionCount((value) => Math.max(5, value - 5))}>−</button><strong>{questionCount}</strong><button type="button" onClick={() => setQuestionCount((value) => Math.min(50, value + 5))}>+</button></div></div><div className="setup-row"><span>题目来源</span><label className="source-check"><input type="checkbox" defaultChecked />仅当前题库</label></div><button type="button" className="primary-action start-practice" onClick={() => { setPracticeStarted(true); setAnswer(''); setResult(null) }}>开始本次练习 <ArrowUpRight size={15} /></button></div> : <div className="practice-session"><div className="practice-progress"><span>作答模式 · {activeLibraryName}</span><strong>01 / {questionCount}</strong></div><div className="practice-question"><span className="section-label">单选题</span><h2>在可达性分析中，哪一项可以作为判断对象存活的起点？</h2>{['A. 线程栈中的所有局部变量', 'B. GC Roots', 'C. 所有堆对象', 'D. 已加载的 Class 文件'].map((option) => <button key={option} type="button" disabled={Boolean(result)} className={`answer-option ${answer === option.slice(0, 1) ? 'is-selected' : ''} ${result && option.startsWith('B') ? 'is-correct' : ''}`} onClick={() => setAnswer(option.slice(0, 1))}><span>{option.slice(0, 1)}</span>{option.slice(3)}</button>)}</div>{result ? <div className={`answer-result ${result}`}><span>{result === 'correct' ? <Check size={16} /> : <X size={16} />}</span><div><strong>{result === 'correct' ? '回答正确' : '这道题已加入错题知识库'}</strong><p>GC Roots 是可达性分析的起点。真实服务会在这里保存记录和解析。</p></div><button type="button" onClick={() => { setPracticeStarted(false); setResult(null) }}>结束演示</button></div> : <button type="button" className="primary-action submit-answer" disabled={!answer} onClick={submitAnswer}>提交答案</button>}</div>}
        </section>
      </section>
    </AppPage>
  )
}

function AboutPage({ navigate }) {
  const focusAreas = [
    { icon: Settings2, title: '后端开发', description: '持续打磨 Java 服务端能力，把接口、数据与业务流程做得清楚、稳定。' },
    { icon: BrainCircuit, title: 'Agent 开发与落地', description: '关注 AI 能力怎样进入真实学习与工具场景，而不止停留在一次对话。' },
    { icon: GraduationCap, title: '算法', description: '保持基础训练，持续把抽象问题拆开、验证，再回到可实现的方案。' },
  ]
  const placeholders = ['项目经历', '实习经历', '技术栈']

  return (
    <AppPage navigate={navigate} current="/about">
      <section className="about-hero" aria-labelledby="about-title">
        <motion.div className="about-nameplate" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, ease: easing }}>
          <img className="about-avatar" src={authorAvatar} alt="瑾瑜的头像" />
          <h1 id="about-title">瑾瑜</h1>
          <p>烟台科技学院 · 计算机专业<br />2028 届本科生 · 学习中</p>
        </motion.div>
        <motion.div className="about-opening" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.12, duration: 0.8, ease: easing }}>
          <blockquote>你才 20 多，<br />做什么都不丢人。<br />什么不做才丢人。</blockquote>
          <p>Java 全栈 / AI 应用开发。主动学习，也愿意把正在走的路和踩过的坑分享出来。</p>
        </motion.div>
      </section>

      <section className="about-facts" aria-label="作者信息">
        <div><span>正在成为</span><strong>Java 全栈开发者与 AI 应用开发者</strong></div>
        <div><span>学习方向</span><strong>后端开发 / Agent 开发与落地 / 算法</strong></div>
        <a href="mailto:3185954515@qq.com"><Mail size={16} />3185954515@qq.com<ArrowUpRight size={15} /></a>
      </section>

      <section className="about-story">
        <div className="about-section-intro">
          <h2>把学习的过程，<br />变成可以一起走的路径。</h2>
        </div>
        <div className="about-story-copy"><p>“狠狠学”从一个很朴素的想法开始：把自己在学习中的文章、心得与想法留下来，也把零散知识整理成可继续追问的结构。</p><p>我希望它既能帮助自己持续学习，也能提供一条由 AI 辅助理解、练习与复盘的路径，让每次投入都有机会变成真正可用的能力。</p><button type="button" className="text-action" onClick={() => navigate('/learn')}>看看 AI 知识树 <ArrowUpRight size={15} /></button></div>
      </section>

      <section className="about-focus" aria-labelledby="focus-title">
        <h2 id="focus-title">正在深耕</h2>
        <div className="about-focus-list">{focusAreas.map(({ icon: Icon, title, description }) => <article className="about-focus-item" key={title}><span className="about-focus-icon"><Icon size={18} strokeWidth={1.8} /></span><div><h3>{title}</h3><p>{description}</p></div></article>)}</div>
      </section>

      <section className="about-placeholders" aria-labelledby="placeholder-title">
        <div className="about-section-intro"><h2 id="placeholder-title">接下来会写进这里。</h2><p>真实经历准备好后，会逐步补全。</p></div>
        <div className="about-placeholder-list">{placeholders.map((title) => <article key={title}><h3>{title}</h3><p>等待瑾瑜补充</p></article>)}</div>
      </section>

      <footer className="about-signoff"><p>持续学习，主动实践，也积极接触 AI。</p><button type="button" className="secondary-action" onClick={() => navigate('/blog')}>阅读博客</button></footer>
    </AppPage>
  )
}

function ProfilePage({ navigate }) {
  const [editing, setEditing] = useState(false)
  const [profile, setProfile] = useState({ name: '瑾瑜', signature: '把复杂的问题，学成可解释的答案。', email: '3185954515@qq.com' })
  return (
    <AppPage navigate={navigate} current="/profile">
      <PageIntro label="个人中心" title={<>你的学习<br />应该有自己的轮廓</>} summary="个人资料、签名和模型偏好都会在这里管理。当前为前端 Mock，保存仅更新当前浏览器状态。"><button type="button" className="secondary-action compact-action" onClick={() => setEditing((value) => !value)}>{editing ? '完成编辑' : '编辑资料'}</button></PageIntro>
      <section className="profile-layout"><aside className="profile-identity"><div className="profile-avatar" aria-label="瑾瑜的头像"><img src={authorAvatar} alt="" /></div><h2>{profile.name}</h2><p>{profile.signature}</p><div className="profile-links"><button type="button" onClick={() => navigate('/blog')}><BookOpen size={15} />我的收藏</button><button type="button" onClick={() => navigate('/questions')}><GraduationCap size={15} />练习记录</button></div></aside><section className="profile-details"><div className="detail-heading"><span className="section-label">公开资料</span><h2>保持简洁，也保持可辨认。</h2></div><div className="profile-form"><label>头像名称<input disabled={!editing} value={profile.name} onChange={(event) => setProfile((value) => ({ ...value, name: event.target.value }))} /></label><label>个人签名<textarea disabled={!editing} value={profile.signature} onChange={(event) => setProfile((value) => ({ ...value, signature: event.target.value }))} /></label><label>邮箱地址<input disabled={!editing} type="email" value={profile.email} onChange={(event) => setProfile((value) => ({ ...value, email: event.target.value }))} /></label>{editing && <button type="button" className="primary-action" onClick={() => setEditing(false)}>保存模拟资料 <Check size={15} /></button>}</div><div className="credential-summary"><span><Sparkles size={15} />已验证模型</span><strong>DeepSeek · deepseek-chat</strong><button type="button">管理模型设置 <ChevronRight size={15} /></button></div></section></section>
    </AppPage>
  )
}

function AuthPage({ mode, navigate }) {
  const [form, setForm] = useState({ account: '', email: '', code: '', password: '' })
  const [notice, setNotice] = useState('')
  const isLogin = mode === 'login'
  const submit = (event) => {
    event.preventDefault()
    if ((isLogin && (!form.account || !form.password)) || (!isLogin && (!form.account || !form.email || !form.code || !form.password))) { setNotice('请先完整填写演示表单。'); return }
    setNotice(isLogin ? '登录信息已校验，正在进入你的学习空间。' : '注册信息已暂存，真实服务接入后将发送邮箱验证码。')
  }
  const update = (key) => (event) => setForm((value) => ({ ...value, [key]: event.target.value }))
  const title = isLogin ? '继续你的学习。' : <>开始建立自己的<br />知识系统。</>

  return (
    <main className="auth-shell"><div className="auth-media"><video autoPlay muted playsInline preload="auto" onEnded={(event) => { const video = event.currentTarget; video.currentTime = Math.max(0, video.duration - 0.08); video.pause() }}><source src={heroVideoUrl} type="video/mp4" /></video><div className="auth-media-wash" /><button type="button" className="auth-back" onClick={() => navigate('/')}><ArrowLeft size={15} />返回首页</button><div className="auth-wordmark"><BrandMark /><span>狠狠学</span></div><p>让问题有路径，<br />让练习有回声。</p></div><section className="auth-panel"><div className="auth-mobile-chrome"><button type="button" className="brand" onClick={() => navigate('/')}><BrandMark /><span>狠狠学</span></button><button type="button" onClick={() => navigate('/')}><ArrowLeft size={14} />首页</button></div><div className="auth-switch"><button type="button" className={isLogin ? 'is-active' : ''} onClick={() => navigate('/login')}>登录</button><button type="button" className={!isLogin ? 'is-active' : ''} onClick={() => navigate('/register')}>注册</button></div><div className="auth-heading"><span className="section-label">{isLogin ? '欢迎回来' : '创建账号'}</span><h1>{title}</h1><p>{isLogin ? '用户名或邮箱都可以登录。' : '使用邮箱完成验证后，就能创建私有题库和知识树。'}</p></div><form className="auth-form" onSubmit={submit}><label>{isLogin ? '用户名或邮箱' : '用户名'}<input value={form.account} onChange={update('account')} placeholder={isLogin ? 'name@example.com' : '8 - 64 位'} /></label>{!isLogin && <label>邮箱地址<input type="email" value={form.email} onChange={update('email')} placeholder="name@example.com" /></label>}{!isLogin && <label>邮箱验证码<div className="code-field"><input value={form.code} onChange={update('code')} placeholder="6 位验证码" /><button type="button">发送验证码</button></div></label>}<label>密码<input type="password" value={form.password} onChange={update('password')} placeholder="8 - 64 位" /></label>{isLogin && <button type="button" className="forgot-password">忘记密码？</button>}<button type="submit" className="primary-action auth-submit">{isLogin ? '登录并继续' : '创建我的账号'} <ArrowUpRight size={15} /></button>{notice && <p className="auth-notice"><Mail size={14} />{notice}</p>}</form></section>
    </main>
  )
}

export default App
