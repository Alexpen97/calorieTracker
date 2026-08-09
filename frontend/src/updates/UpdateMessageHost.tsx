import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acknowledgeUpdateMessage,
  fetchPendingUpdateMessage,
} from '../api/client'
import UpdateMessageCard from './UpdateMessageCard'

export default function UpdateMessageHost() {
  const queryClient = useQueryClient()
  const pending = useQuery({
    queryKey: ['update-message-pending'],
    queryFn: fetchPendingUpdateMessage,
  })

  const ack = useMutation({
    mutationFn: (id: string) => acknowledgeUpdateMessage(id),
    onSuccess: () => {
      queryClient.setQueryData(['update-message-pending'], null)
    },
  })

  if (!pending.data) {
    return null
  }

  const message = pending.data

  return (
    <UpdateMessageCard
      message={message}
      dismissing={ack.isPending}
      onDismiss={() => {
        if (ack.isPending) {
          return
        }
        ack.mutate(message.id)
      }}
    />
  )
}
