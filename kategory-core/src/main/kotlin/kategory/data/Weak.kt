package kategory

import java.lang.ref.WeakReference

/**
 * Represents an object that can stop existing when no references to it are present. It is backed by
 * a [WeakReference] instance.
 */
@higherkind
@deriving(
    Functor::class,
    Applicative::class,
    Monad::class,
    Foldable::class,
    Traverse::class,
    TraverseFilter::class,
    MonadFilter::class)
data class Weak<out A>(internal val provider: () -> A?) : WeakKind<A> {

    companion object {

        private val EMPTY: Weak<Nothing> = Weak({ null })

        @Suppress("UNCHECKED_CAST")
        fun <B> emptyWeak(): Weak<B> = EMPTY

        operator fun <A> invoke(a: A): Weak<A> {
            val reference = WeakReference(a)
            return Weak { reference.get() }
        }
    }

    fun <B> fold(fn: () -> B, f: (A) -> B): B
        = provider()?.let(f) ?: fn()

    inline fun <B> map(crossinline f: (A) -> B): Weak<B> = fold({ emptyWeak() }, { Weak(f(it)) })

    inline fun <B> flatMap(crossinline f: (A) -> Weak<B>): Weak<B> = fold({ emptyWeak() }, { a -> f(a) })

    inline fun filter(crossinline p: (A) -> Boolean): Weak<A> = fold({ emptyWeak() }, { a -> if (p(a)) a.weak() else emptyWeak<A>() })

}

/**
 * Returns the internal value or an alternative if we've lost it.
 *
 * @param fallback provides a new value if we have lost the current one.
 */
fun <B> Weak<B>.getOrElse(fallback: () -> B): B = fold({ fallback() }, { it })

/**
 * Returns this Weak instance if present or an alternative.
 *
 * @param fallback provides a new value if we have lost the current one.
 */
fun <A, B : A> WeakKind<B>.orElse(fallback: () -> Weak<B>): Weak<B> = ev().provider()?.let { ev() } ?: fallback()

fun <A> A.weak(): Weak<A> = Weak(this)