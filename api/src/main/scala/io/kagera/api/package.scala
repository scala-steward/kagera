package io.kagera

import scalaz.@@

package object api {

  type Marking[P] = Map[P, Long]

  object tags {
    trait Id
    trait Weight
    trait Label
  }

  type Identifiable[T] = T ⇒ Long @@ tags.Id
  type Labeled[T] = T ⇒ String @@ tags.Label

  implicit class LabeledFn[T: Labeled](seq: Iterable[T]) {
    def findByLabel(label: String) = seq.find(e ⇒ implicitly[Labeled[T]].apply(e) == label)
  }

  implicit class IdFn[T: Identifiable](seq: Iterable[T]) {
    def findById(id: String) = seq.find(e ⇒ implicitly[Identifiable[T]].apply(e) == id)
  }

  type PTProcess[P, T, M] = PetriNet[P, T] with TokenGame[P, T, M] with TransitionExecutor[P, T, M]

  // given a process and current marking picks the next transition and marking to fire
  type Step[P, T, M] = (PTProcess[P, T, M], M) ⇒ Option[(M, T)]

  /**
   * Type class for marking 'like' semantics.
   */
  trait MarkingLike[M, P] {

    def emptyMarking: M

    def multiplicity(marking: M): Marking[P]

    def isSubMarking(m: M, other: M): Boolean

    def consume(from: M, other: M): M

    def produce(into: M, other: M): M
  }

  implicit class MarkingLikeApi[M, P](val m: M)(implicit val markingLike: MarkingLike[M, P]) {
    def consume(other: M) = markingLike.consume(m, other)
    def produce(other: M) = markingLike.produce(m, other)
    def isEmpty() = markingLike.multiplicity(m).isEmpty
    def isSubMarking(other: M) = markingLike.isSubMarking(m, other)
  }

  trait TransitionExecutor[P, T, M] {

    this: PetriNet[P, T] ⇒

    def fireTransition(marking: M)(transition: T): M
  }

  trait TokenGame[P, T, M] {

    this: PetriNet[P, T] ⇒

    def enabledParameters(m: M): Map[T, Iterable[M]] = {
      // inefficient, fix
      enabledTransitions(m).view.map(t ⇒ t -> consumableMarkings(m)(t)).toMap
    }

    def consumableMarkings(m: M)(t: T): Iterable[M]

    // horribly inefficient, fix
    def isEnabled(marking: M)(t: T): Boolean = enabledTransitions(marking).contains(t)

    def enabledTransitions(marking: M): Set[T]
  }
}
