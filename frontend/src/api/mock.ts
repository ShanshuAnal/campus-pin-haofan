import type { LoginResponse, UserSummary } from '@/types/auth'
import type {
  DashboardSummary,
  GroupOrderDetail,
  GroupOrderSummary,
  MyGroupOrderRecord,
  Participant,
  PickupRecord
} from '@/types/order'

export const currentUser: UserSummary = {
  id: 1001,
  username: '20260001',
  nickname: '小何',
  phone: null,
  status: 'ACTIVE'
}

const users: UserSummary[] = [
  currentUser,
  { id: 1002, username: '20260002', nickname: '小林', phone: null, status: 'ACTIVE' },
  { id: 1003, username: '20260003', nickname: '小周', phone: null, status: 'ACTIVE' },
  { id: 1004, username: '20260004', nickname: '小陈', phone: null, status: 'ACTIVE' }
]

export const mockOrders: GroupOrderSummary[] = [
  {
    id: 2001,
    title: '奶茶满 60 减 10',
    orderType: 'MILK_TEA',
    merchantName: '一号门奶茶',
    pickupLocation: '一教大厅门口',
    creator: users[0],
    deadlineTime: '2026-05-28 19:30:00',
    maxParticipants: 5,
    participantCount: 3,
    discountThresholdAmount: 60,
    discountAmount: 10,
    originalTotalAmount: 68,
    actualDiscountAmount: 10,
    payableTotalAmount: 58,
    roundingAdjustmentAmount: 0.01,
    status: 'LOCKED',
    pickupUser: users[1],
    remark: '统一备注甜度，锁单后线下转账',
    lockedTime: '2026-05-28 18:31:00',
    finishTime: null,
    cancelTime: null,
    createTime: '2026-05-28 18:02:00',
    updateTime: '2026-05-28 18:31:00'
  },
  {
    id: 2002,
    title: '二食堂黄焖鸡凑单',
    orderType: 'CANTEEN',
    merchantName: '二食堂一楼',
    pickupLocation: '二食堂南门',
    creator: users[2],
    deadlineTime: '2026-05-28 20:00:00',
    maxParticipants: 4,
    participantCount: 2,
    discountThresholdAmount: 0,
    discountAmount: 0,
    originalTotalAmount: 41,
    actualDiscountAmount: 0,
    payableTotalAmount: 41,
    roundingAdjustmentAmount: 0,
    status: 'CREATED',
    pickupUser: null,
    remark: '下课后一起取',
    lockedTime: null,
    finishTime: null,
    cancelTime: null,
    createTime: '2026-05-28 17:45:00',
    updateTime: '2026-05-28 17:45:00'
  },
  {
    id: 2003,
    title: '夜宵炸串拼单',
    orderType: 'MIDNIGHT_SNACK',
    merchantName: '北门炸串',
    pickupLocation: '宿舍 C 区门口',
    creator: users[3],
    deadlineTime: '2026-05-28 22:10:00',
    maxParticipants: 6,
    participantCount: 4,
    discountThresholdAmount: 80,
    discountAmount: 12,
    originalTotalAmount: 94,
    actualDiscountAmount: 12,
    payableTotalAmount: 82,
    roundingAdjustmentAmount: 0,
    status: 'ARRIVED',
    pickupUser: users[0],
    remark: '餐到了在群里喊',
    lockedTime: '2026-05-28 21:20:00',
    finishTime: null,
    cancelTime: null,
    createTime: '2026-05-28 20:50:00',
    updateTime: '2026-05-28 21:55:00'
  }
]

const participantsByOrder: Record<number, Participant[]> = {
  2001: [
    {
      id: 3001,
      groupOrderId: 2001,
      user: users[0],
      originalAmount: 22,
      discountShareAmount: 3.24,
      payableAmount: 18.77,
      roundingAdjustmentAmount: 0.01,
      paymentStatus: 'CONFIRMED',
      paidMarkTime: '2026-05-28 18:40:00',
      paidConfirmTime: '2026-05-28 18:43:00',
      joinTime: '2026-05-28 18:08:00',
      remark: '少冰，不要吸管',
      mealItems: [
        {
          id: 4001,
          groupOrderId: 2001,
          participantId: 3001,
          itemName: '珍珠奶茶',
          quantity: 1,
          unitPrice: 22,
          subtotalAmount: 22,
          remark: '少冰'
        }
      ]
    },
    {
      id: 3002,
      groupOrderId: 2001,
      user: users[1],
      originalAmount: 24,
      discountShareAmount: 3.53,
      payableAmount: 20.47,
      roundingAdjustmentAmount: 0,
      paymentStatus: 'PAID',
      paidMarkTime: '2026-05-28 18:41:00',
      paidConfirmTime: null,
      joinTime: '2026-05-28 18:12:00',
      remark: '半糖',
      mealItems: [
        {
          id: 4002,
          groupOrderId: 2001,
          participantId: 3002,
          itemName: '芝士绿茶',
          quantity: 1,
          unitPrice: 24,
          subtotalAmount: 24,
          remark: '半糖'
        }
      ]
    },
    {
      id: 3003,
      groupOrderId: 2001,
      user: users[2],
      originalAmount: 22,
      discountShareAmount: 3.23,
      payableAmount: 18.76,
      roundingAdjustmentAmount: 0,
      paymentStatus: 'UNPAID',
      paidMarkTime: null,
      paidConfirmTime: null,
      joinTime: '2026-05-28 18:15:00',
      remark: '正常冰',
      mealItems: [
        {
          id: 4003,
          groupOrderId: 2001,
          participantId: 3003,
          itemName: '柠檬红茶',
          quantity: 1,
          unitPrice: 22,
          subtotalAmount: 22,
          remark: '正常冰'
        }
      ]
    }
  ]
}

const pickupRecord: PickupRecord = {
  id: 6001,
  groupOrderId: 2001,
  pickupUser: users[1],
  pickupLocation: '一教大厅门口',
  pickupStatus: 'WAITING_ORDER',
  estimatedArrivalTime: '2026-05-28 19:10:00',
  actualArrivalTime: null,
  pickedUpTime: null,
  distributedTime: null,
  remark: '小林去取'
}

export const mockLogin = async (): Promise<LoginResponse> => ({
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  expiresIn: 7200,
  user: currentUser
})

export const fetchGroupOrdersMock = async (): Promise<GroupOrderSummary[]> => [...mockOrders]

export const fetchGroupOrderDetailMock = async (id: number): Promise<GroupOrderDetail> => {
  const order = mockOrders.find((item) => item.id === id) ?? mockOrders[0]
  return {
    order,
    participants: participantsByOrder[order.id] ?? [],
    pickupRecord: order.id === 2001 ? pickupRecord : null
  }
}

export const fetchMyOrdersMock = async (): Promise<MyGroupOrderRecord[]> =>
  mockOrders.map((order, index) => ({
    order,
    myRole: index === 0 ? 'CREATOR' : index === 2 ? 'PICKUP_USER' : 'PARTICIPANT',
    myParticipantId: index === 1 ? 3008 : index === 0 ? 3001 : null,
    myPayableAmount: index === 0 ? 18.77 : index === 1 ? 21 : 0,
    myPaymentStatus: index === 2 ? null : index === 0 ? 'CONFIRMED' : 'UNPAID',
    pickupStatus: index === 2 ? 'ARRIVED' : index === 0 ? 'WAITING_ORDER' : null
  }))

export const fetchDashboardSummaryMock = async (): Promise<DashboardSummary> => ({
  orderCount: 12,
  createdCount: 3,
  lockedCount: 2,
  finishedCount: 5,
  cancelledCount: 2,
  participantCount: 36,
  originalTotalAmount: 860,
  actualDiscountAmount: 90,
  payableTotalAmount: 770,
  paidParticipantCount: 20,
  confirmedParticipantCount: 18
})
