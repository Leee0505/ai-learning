import { ElMessage } from 'element-plus'

const defaults = { duration: 1500, showClose: false }

export function messageSuccess(msg) { return ElMessage.success({ message: msg, ...defaults }) }
export function messageError(msg) { return ElMessage.error({ message: msg, ...defaults }) }
export function messageWarning(msg) { return ElMessage.warning({ message: msg, ...defaults, duration: 2000 }) }
export function messageInfo(msg) { return ElMessage.info({ message: msg, ...defaults }) }
