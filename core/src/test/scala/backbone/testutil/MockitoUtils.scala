package backbone.testutil

import org.mockito.ArgumentCaptor
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatestplus.mockito.MockitoSugar

import scala.reflect.ClassTag

trait MockitoUtils extends MockitoSugar {

  def answer[T](block: InvocationOnMock => T): Answer[T] = new Answer[T] {
    override def answer(invocation: InvocationOnMock): T = block(invocation)
  }

  def argumentCaptor[A](implicit classTag: ClassTag[A]): ArgumentCaptor[A] =
    ArgumentCaptor.forClass(classTag.runtimeClass.asInstanceOf[Class[A]])

}
