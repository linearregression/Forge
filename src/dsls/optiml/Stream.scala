package ppl.dsl.forge
package dsls
package optiml

import core.{ForgeApplication,ForgeApplicationRunner}

/**
 * Stream provides basic iterator functionality over a file or computation too
 * large to fit in memory.
 */
trait StreamOps {
  this: OptiMLDSL =>

  def importStreamOps() {
    importFileStreamOps()
    importComputeStreamOps()
  }

  def importFileStreamOps() {
    val Stream = tpe("FileStream")
    
    data(Stream, ("_path", MString))

    static (Stream) ("apply", Nil, MString :: Stream) implements allocates(Stream, ${$0})
    
    // we need to read sequentially, but from potentially different data stores, so we use ForgeFileInputStream and ForgeFileOutputStream
    
    val T = tpePar("T")
    val StreamOps = withTpe(Stream)
    StreamOps {
      infix ("path") (Nil :: MString) implements getter(0, "_path")

      // currently loaded and executed sequentially, chunk-by-chunk
      infix ("foreach") ((MString ==> MUnit) :: MUnit) implements single ${        
        val f = ForgeFileInputStream($self.path)
        var line = f.readLine()
        while (line != null) {
          $1(line)
          line = f.readLine()
        }
        f.close()
      }

      infix ("map") (CurriedMethodSignature(List(List(("outFile", MString)), List(("func", MString ==> MString))), MUnit)) implements composite ${      
        val out = ForgeFileOutputStream(outFile)
        // the below incorrectly infers the type of 'line' for mysterious reasons
        // for (line <- $self) { 
        $self.foreach { line: Rep[String] => 
          out.writeLine(func(line))
        }        
        out.close()
      } 

      infix ("reduce") (CurriedMethodSignature(List(List(("init", T)), List(("func", (T,MString) ==> T))), T), addTpePars = T) implements composite ${
        var acc = init
        // for (line <- $self) { 
        $self.foreach { line: Rep[String] => 
          acc = func(acc, line)
        }
        acc
      }
    }
  }  

  def importComputeStreamOps() {
    val T = tpePar("T")
    val DenseVectorView = lookupTpe("DenseVectorView")
    val Tup2 = lookupTpe("Tup2")
    val Stream = tpe("ComputeStream", T)    

    data(Stream, ("_numRows", MInt), ("_numCols", MInt), ("_func", MLambda(Tup2(MInt,MInt), T)))

    static (Stream) ("apply", T, (CurriedMethodSignature(List(List(("numRows", MInt), ("numCols", MInt)), List(("func", (MInt,MInt) ==> T))), Stream(T)))) implements allocates(Stream, ${$0}, ${$1}, ${doLambda((t: Rep[Tup2[Int,Int]]) => func(t._1, t._2))})

    val StreamOps = withTpe(Stream)
    StreamOps {
      infix ("numRows") (Nil :: MInt) implements getter(0, "_numRows")
      infix ("numCols") (Nil :: MInt) implements getter(0, "_numCols")
      compiler ("stream_func") (Nil :: MLambda(Tup2(MInt,MInt), T)) implements getter(0, "_func")

      infix ("apply") ((MInt, MInt) :: T) implements composite ${
        val lambda = stream_func($self)
        doApply(lambda, pack(($1,$2)))
      }

      infix ("foreach") ((T ==> MUnit) :: MUnit) implements composite ${        
        (0::$self.numRows) foreach { i =>
          // the below produces a could not find "::" value error for mysterious reasons
          // (0::$self.numCols) foreach { j =>
          IndexVector(0, $self.numCols) foreach { j =>
            $1($self(i,j))                      
          }
        }
      }
      
      infix ("foreachRow") ((DenseVectorView(T) ==> MUnit) :: MUnit) implements composite ${        
        // buffered to avoid producing a large amount of garbage (instantiating a row each time)
        // we use a matrix instead of a single vector as a buffer to increase parallelism
        val chunkSize = ceil(1000000/$self.numCols) // heuristic for number of rows to process at one time. total buffer size is chunkSize x numCols
        val buf = DenseMatrix[T](chunkSize, $self.numCols)
        val numChunks = ceil($self.numRows / chunkSize.toDouble)

        var chunkIdx = 0
        while (chunkIdx < numChunks) {
          val remainingRows = $self.numRows - chunkIdx*chunkSize
          val leftover = if (remainingRows < 0) $self.numRows else remainingRows // in case numRows < chunkSize
          val rowsToProcess = min(chunkSize, leftover)
          (0::rowsToProcess) foreach { i =>
            // (0::$self.numCols) foreach { j =>
            IndexVector(0, $self.numCols) foreach { j =>            
              buf(i,j) = $self(i,j)
            }            
            $1(buf(i))
          }          

          chunkIdx += 1
        }
      }
    }        
  } 
}
