import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

function readView(name) {
  return readFileSync(join(process.cwd(), 'src', 'views', `${name}.vue`), 'utf-8')
}

describe('user experience regression guards', () => {
  it('uses an in-page partner application form instead of native prompt', () => {
    const source = readView('Partner')

    expect(source).not.toContain('prompt(')
    expect(source).toContain('申请附言（可选）')
    expect(source).toContain('submitApply')
  })

  it('shows review content snapshots in the admin review list', () => {
    const source = readView('Admin')

    expect(source).toContain('reviewSnapshot(r)')
    expect(source).toContain('contentSnapshot')
    expect(source).toContain('contentTypeLabel')
  })

  it('normalizes review result notifications and opens details from the view button', () => {
    const source = readView('Notifications')

    expect(source).toContain('notificationContent(n)')
    expect(source).toContain('PASSED')
    expect(source).toContain('已通过')
    expect(source).toContain('查看详情')
    expect(source).toContain('@click.stop="openNotification(n)"')
  })
})
