package backbone.aws

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.concurrent.{Future, Promise}

private[backbone] trait AmazonAsync {

  def async[A <: AmazonWebServiceRequest, B](f: AsyncHandler[A, B] => Any): Future[B] = {

    val p = Promise[B]()

    f(new AsyncHandler[A, B] {
      override def onError(e: Exception): Unit            = p.failure(e)
      override def onSuccess(request: A, result: B): Unit = p.success(result)
    })

    p.future

  }

}
