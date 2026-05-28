import {
  fetchDashboardSummaryMock,
  fetchGroupOrderDetailMock,
  fetchGroupOrdersMock,
  fetchMyOrdersMock
} from './mock'

export const orderApi = {
  listGroupOrders: fetchGroupOrdersMock,
  getGroupOrderDetail: fetchGroupOrderDetailMock,
  listMyOrders: fetchMyOrdersMock,
  getDashboardSummary: fetchDashboardSummaryMock
}
