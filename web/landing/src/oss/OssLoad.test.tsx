import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import Oss from './Oss'

const load = vi.hoisted(() => vi.fn<(key: string) => Promise<string>>())
vi.mock('./licenses', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./licenses')>()),
  loadLicenseText: load,
}))

it('전문을 못 불러오면 안내하고, 다시 펼칠 때 또 부른다', async () => {
  load.mockRejectedValueOnce(new Error('chunk')).mockResolvedValueOnce('MIT License text')
  render(<Oss platform={{ os: 'ios', inApp: false, kakao: false }} />)
  const summary = screen.getByText('Alamofire')

  await userEvent.click(summary)
  expect(await screen.findByText(/전문을 불러오지 못했어요/)).toBeInTheDocument()

  await userEvent.click(summary)
  await userEvent.click(summary)
  expect(await screen.findByText('MIT License text')).toBeInTheDocument()
  expect(load).toHaveBeenCalledTimes(2)
  expect(load).toHaveBeenCalledWith('alamofire')
})
