package com.salesnetwork.avon.app

import com.salesnetwork.avon.app.domain.model.OrderStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusTest {
    @Test
    fun `commercial flow only accepts audited forward transitions`() {
        assertTrue(OrderStatus.PENDIENTE.canTransitionTo(OrderStatus.CONFIRMADO))
        assertTrue(OrderStatus.CONFIRMADO.canTransitionTo(OrderStatus.COBRADO))
        assertTrue(OrderStatus.COBRADO.canTransitionTo(OrderStatus.ENTREGADO))
        assertTrue(OrderStatus.ENTREGADO.canTransitionTo(OrderStatus.DEVUELTO))
        assertFalse(OrderStatus.ENTREGADO.canTransitionTo(OrderStatus.COBRADO))
        assertFalse(OrderStatus.CANCELADO.canTransitionTo(OrderStatus.PENDIENTE))
        assertFalse(OrderStatus.DEVUELTO.canTransitionTo(OrderStatus.ENTREGADO))
    }

    @Test
    fun `cancellation is available only before delivery`() {
        assertTrue(OrderStatus.PENDIENTE.canTransitionTo(OrderStatus.CANCELADO))
        assertTrue(OrderStatus.CONFIRMADO.canTransitionTo(OrderStatus.CANCELADO))
        assertTrue(OrderStatus.COBRADO.canTransitionTo(OrderStatus.CANCELADO))
        assertFalse(OrderStatus.ENTREGADO.canTransitionTo(OrderStatus.CANCELADO))
    }
}
