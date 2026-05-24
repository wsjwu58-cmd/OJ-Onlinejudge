import auth from './auth'
import problems from './problems'
import users from './users'
import contests from './contests'
import categories from './categories'
import submissions from './submissions'
import testcases from './testcases'
import groups from './groups'
import common from './common'

// 导出所有API
export {
  auth,
  problems,
  users,
  contests,
  categories,
  submissions,
  testcases,
  groups,
  common
}

// 导出默认对象
export default {
  auth,
  problems,
  users,
  contests,
  categories,
  submissions,
  testcases,
  groups,
  common
}

// 单独导出各个API函数
export * from './auth'
export * from './problems'
export * from './users'
export * from './contests'
export * from './categories'
export * from './submissions'
export * from './testcases'
export * from './groups'
export * from './common';
