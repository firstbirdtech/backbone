package backbone.aws

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

private[backbone] trait AmazonAsync {

  private val logger = LoggerFactory.getLogger(getClass)

  def async[A <: AmazonWebServiceRequest, B](f: AsyncHandler[A, B] => Any): Future[B] = {

    val p = Promise[B]()

    f(new AsyncHandler[A, B] {
      override def onError(e: Exception): Unit = {
        logger.error("Asynchronous request to AWS failed.", e)
        p.failure(e)
      }
      override def onSuccess(request: A, result: B): Unit = {
        logger.debug(s"AWS Request $request resulted in $result")
        p.success(result)
      }
    })

    p.future

  }

}
