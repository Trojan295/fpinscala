package fpinscala.laziness

import Stream._
trait Stream[+A] {

  def foldRight[B](z: => B)(f: (A, => B) => B): B = // The arrow `=>` in front of the argument type `B` means that the function `f` takes its second argument by name and may choose not to evaluate it.
    this match {
      case Cons(h,t) => f(h(), t().foldRight(z)(f)) // If `f` doesn't evaluate its second argument, the recursion never occurs.
      case _ => z
    }

  def exists(p: A => Boolean): Boolean = 
    foldRight(false)((a, b) => p(a) || b) // Here `b` is the unevaluated recursive step that folds the tail of the stream. If `p(a)` returns `true`, `b` will never be evaluated and the computation terminates early.

  @annotation.tailrec
  final def find(f: A => Boolean): Option[A] = this match {
    case Empty => None
    case Cons(h, t) => if (f(h())) Some(h()) else t().find(f)
  }

  def toList: List[A] = this match {
    case Empty => Nil
    case Cons(h, t) => h() :: t().toList
  }

  def take(n: Int): Stream[A] = this match {
    case Empty => Empty
    case Cons(h, t) =>
      if (n==0) Empty
      else Cons(h, () => t().take(n-1))
  }

  def drop(n: Int): Stream[A] = this match {
    case Empty => Empty
    case Cons(h, t) =>
      if (n==0) Cons(h, t)
      else t().drop(n-1)
  }

  def takeWhile(p: A => Boolean): Stream[A] = 
    foldRight( Empty: Stream[A] )( (a, b) => {
      println(s"Folding $a")
      if (p(a)) Cons(() => a, () => b)
      else Empty
    } )

  def forAll(p: A => Boolean): Boolean = {
    foldRight(true)( (a, b) => p(a) && b )
  }

  def headOption: Option[A] = foldRight(None: Option[A])( (a,_) => Some(a) )

  def map[B](f: A => B): Stream[B] = foldRight(empty[B])( (a,b) => Cons(() => f(a), () => b) )

  def filter(f: A => Boolean): Stream[A] = foldRight (empty[A]) ( (a,b) =>
      if (f(a)) cons(a, b)
      else b
    )

  def append[B >: A](stream: Stream[B]): Stream[B] = 
    foldRight(stream)( (a,b) => cons(a, b) )

  def flatMap[B](f: A => Stream[B]): Stream[B] = 
    foldRight(empty[B]) ( (a,b) => f(a).append(b) )

  // 5.7 map, filter, append, flatmap using foldRight. Part of the exercise is
  // writing your own function signatures.

  def startsWith[B](s: Stream[B]): Boolean = ???

}
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty[A]: Stream[A] = Empty

  def apply[A](as: A*): Stream[A] =
    if (as.isEmpty) empty 
    else cons(as.head, apply(as.tail: _*))

  val ones: Stream[Int] = Stream.cons(1, ones)
  def from(n: Int): Stream[Int] = ???

  def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = ???

  def main(args: Array[String]): Unit = {
    val s = Stream(1,2,3,4)
    val r = s flatMap (x => Cons(() => x+1, () => Empty))
    println(r.toList)
  }
}
