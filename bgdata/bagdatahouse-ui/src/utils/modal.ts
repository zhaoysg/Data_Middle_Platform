import { Modal } from 'ant-design-vue'

/**
 * 确认对话框封装
 * @param content 确认内容
 * @param title 标题（可选）
 * @returns Promise<boolean>
 */
export function confirmModal(content: string, title = '确认操作'): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title,
      content,
      okText: '确定',
      cancelText: '取消',
      onOk() {
        resolve(true)
      },
      onCancel() {
        resolve(false)
      }
    })
  })
}
