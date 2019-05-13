package com.snapptrip.redis

import com.typesafe.scalalogging.LazyLogging

object RedisClient extends LazyLogging {

//  private val redis = RedisConfig.redis
//  private val nameSpace = RedisConfig.adminNameSpace
//  private val refundRequestNameSpace = s"$nameSpace:refund"

//  def existRefundRequestModel(refundRequestModel: RefundRequestModel): Future[Boolean] = {
//    logger.info("validate requested refund :")
//    logger.info(refundRequestModel.toString)
//    val nameSpace = s"$refundRequestNameSpace:${refundRequestModel.refund_partner_order_id}"
//    redis.exists(nameSpace)
//      .andThen {
//        case Success(true)      =>
//          logger.info("refundPartnerOrderId exists")
//          redis.del(nameSpace)
//        case Success(false)     =>
//          logger.error(s"refundPartnerOrderId does not exists:${refundRequestModel.refund_partner_order_id}")
//        case Failure(exception) =>
//          logger.error("error while getting refund", exception)
//      }
//  }
//
//  def saveRefundRequestModel(refundRequestModel: RefundRequestModel): Future[Boolean] = {
//    val nameSpace = s"$refundRequestNameSpace:${refundRequestModel.refund_partner_order_id}"
//    for {
//      result <- redis.set[String](nameSpace, s"${refundRequestModel.refund_partner_order_id}")
//      _ <- redis.expire(nameSpace, 10 * 60)
//    } yield {
//      result
//    }
//  }

}