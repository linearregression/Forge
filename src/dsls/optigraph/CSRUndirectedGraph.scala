/*//////////////////////////////////////////////////////////////
Author: Christopher R. Aberger

Description: The main file for all CSRUndirectedGraph operations.  Glues 
togther all structures and declares CSRUndirectedGraph operations visible
to user. Inherits from Graph.scala

Data is stored the same as in a directed graph but we only store
out edges. In an undirected graph in=out edges.
*///////////////////////////////////////////////////////////////
package ppl.dsl.forge
package dsls 
package optigraph

import core.{ForgeApplication,ForgeApplicationRunner}

trait CSRUndirectedGraphOps{
  this: OptiGraphDSL =>

  def importCSRUndirectedGraphOps() {
    //previously declared types we use
    val Node = lookupTpe("Node")
    val Edge = lookupTpe("Edge")
    val NodeData = lookupTpe("NodeData")
    val NodeDataView = lookupTpe("NodeDataView")
    val NodeIdView = lookupTpe("NodeIdView")
    val RoaringBitmap = ephemeralTpe("org.roaringbitmap.RoaringBitmap")

    //Actual CSRUndirectedGraph declaration
    val CSRUndirectedGraph = tpe("CSRUndirectedGraph") 
    val T = tpePar("T")
    val R = tpePar("R")
    val K = tpePar("K")
    val V = tpePar("V")

    data(CSRUndirectedGraph,("_numNodes",MInt),("_externalIDs",MArray(MInt)),("_nodes",MArray(MInt)),("_edges",MArray(MInt))) 
    static(CSRUndirectedGraph)("apply", Nil, (MethodSignature(List(("count",MInt),("exID",MArray(MInt)),("outNodes",MArray(MInt)),("outEdges",MArray(MInt))), CSRUndirectedGraph))) implements allocates(CSRUndirectedGraph,${$count},${$exID},${$outNodes},${outEdges})

    val CSRUndirectedGraphOps = withTpe(CSRUndirectedGraph)     
    CSRUndirectedGraphOps{
      infix ("numEdges")(Nil :: MInt) implements single ${array_length(edge_raw_data($self))}
      //CSRUndirectedGraph directed or not?
      infix ("isDirected") (Nil :: MBoolean) implements single ${false}
      //Perform a sum over the neighbors
      infix ("sumOverNbrs") ( CurriedMethodSignature(List(("n",Node),("data",MInt==>R),("cond",MInt==>MBoolean)),R), TNumeric(R), addTpePars=R) implements composite ${
        sumOverCollection($self.neighbors(n))(data)(cond)
      }
      //Perform a sum over the neighbors
      infix ("sumOverNbrs") ( CurriedMethodSignature(List(("n",MInt),("data",MInt==>R),("cond",MInt==>MBoolean)),R), TNumeric(R), addTpePars=R) implements composite ${
        sumOverCollection($self.neighbors(n))(data)(cond)
      }
      infix ("sumDownNbrs") ( CurriedMethodSignature(List(List(("n",Node),("level",NodeData(MInt))),("data",MInt==>R)),R), TFractional(R), addTpePars=R) implements composite ${
        //only sum in neighbors a level up
        sumOverCollection($self.outNbrs(n))(data){e => (level(e)==(level(n.id)+1))}
      }
      infix ("sumUpNbrs") ( CurriedMethodSignature(List(List(("n",Node),("level",NodeData(MInt))),("data",MInt==>R)),R), TFractional(R), addTpePars=R) implements composite ${
        sumOverCollection($self.inNbrs(n))(data){e => (level(e)==(level(n.id)-1))}
      }
      infix ("outDegree") (Node :: MInt) implements single ${
        val end  = if( ($1.id+1) < array_length(node_raw_data($self)) ) node_apply($self,($1.id+1)) 
          else array_length(edge_raw_data($self))
        end - node_apply($self,$1.id) 
      }
      infix("returnBitmaps")(Nil :: NodeData(RoaringBitmap)) implements composite ${
        $self.mapNodes{ n =>
          FRoaringBitmap( NodeData($self.neighbors(n).getRawArray).filter( e => e < n.id, e => e).getRawArray )
        }
      }
      infix("countBitmapTriangles")(NodeData(RoaringBitmap) :: MLong) implements composite ${
        NodeIdView($self.numNodes).mapreduce[Long]({n => 
          val nbrs = $1(n)
          var count = 0l
          val bs = clone(nbrs)
          foreach(nbrs,{nbr => 
            //if(nbr > n){
              //andInPlace(bs,$1(nbr))
              count += getCardinality(and($1(nbr),nbrs)).toLong
            //}
          })
          count
          //getCardinality(bs).toLong
        },{(a,b) => a+b},{e=>true})
      }
      infix ("inDegree") (Node :: MInt) implements single ${$self.outDegree($1)}
      //get out neighbors
      infix ("outNbrs") (Node :: NodeDataView(MInt)) implements single ${get_nbrs($self,$1)} 
      infix ("inNbrs") (Node :: NodeDataView(MInt)) implements single ${get_nbrs($self,$1)}
      infix ("neighbors") (MInt :: NodeDataView(MInt)) implements single ${get_nbrs($self,Node($1))}
      infix ("neighbors") (Node :: NodeDataView(MInt)) implements single ${get_nbrs($self,$1)}
      compiler ("get_nbrs") (Node :: NodeDataView(MInt)) implements single ${
        val start = node_apply($self,$1.id)
        val end = if( ($1.id+1) < array_length(node_raw_data($self)) ) node_apply($self,($1.id+1))
          else array_length(edge_raw_data($self))
        NodeDataView[Int](edge_raw_data($self),start,end-start)
      }

      compiler ("node_raw_data") (Nil :: MArray(MInt)) implements getter(0, "_nodes")
      compiler("node_apply")(MInt :: MInt) implements single ${array_apply(node_raw_data($self),$1)}
      compiler ("edge_raw_data") (Nil :: MArray(MInt)) implements getter(0, "_edges")
      compiler("edge_apply")(MInt :: MInt) implements single ${array_apply(edge_raw_data($self),$1)}
    }
    addGraphCommonOps(CSRUndirectedGraph) 
  } 
}
