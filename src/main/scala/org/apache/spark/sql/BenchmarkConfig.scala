/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql

import scala.collection.mutable

// TODO: use SQLConf style i.e. (value, defaultValue)
class BenchmarkConfig {
  // Benchmark config, include file format, index use or not, etc.
  private val benchmarkConf: mutable.HashMap[String, String] = mutable.HashMap.empty

  // SQLConf, send to Spark to change the sql query behavior.
  private val sqlConf: mutable.HashMap[String, String] = mutable.HashMap.empty

  // Spark conf, to initial spark session.
  private val sparkConf: mutable.HashMap[String, String] = mutable.HashMap.empty

  def setBenchmarkConf(name: String, value: String): BenchmarkConfig = {
    benchmarkConf.put(name, value)
    this
  }

  /** A meaningful name for this config
   * like "oap + index" or "parquet w/o index" or "oap and oapStrategy enable"
   */
  def setBenchmarkConfName(name: String): BenchmarkConfig = {
    confName = Option(name)
    this
  }

  var confName: Option[String] = None

  def setSqlConf(name: String, value: String): BenchmarkConfig = {
    sqlConf.put(name, value)
    this
  }

  def setSparkConf(name: String, value: String): BenchmarkConfig = {
    sparkConf.put(name, value)
    this
  }

  /**
   *  Find a conf from all conf settings.
   */
  def getConf(name: String): String = {
    benchmarkConf.get(name).getOrElse(
      sqlConf.get(name).getOrElse(
        sparkConf.get(name).getOrElse(
          s"$name Not Exist!!!")))
  }

  /**
   * Get benchmark config
   * @param name: name
   * @return benchmark config setting.
   */
  def getBenchmarkConf(name: String): String = benchmarkConf.getOrElse(name, "false")

  /**
   * Get sql config
   * @param name: name
   * @return sql config setting.
   */
  def getSqlConf(name: String): String = sqlConf.getOrElse(name, "false")

  /**
   * Get spark config
   * @param name: name
   * @return sql config setting.
   */
  def getSparkConf(name: String): String = sparkConf.getOrElse(name, "false")

  /**
   * Get all sql config
   * @return all sql config settings.
   */
  def allSqlOptions(): Map[String, String] = sqlConf.toMap[String, String]

  /**
   * Get all spark config
   * @return all spark config settings.
   */
  def allSparkOptions(): Map[String, String] = sparkConf.toMap[String, String]

  /**
   * Make config settings as config name, used if none name set.
   * @return
   */
  def configString: String = {
    if (sqlConf.isEmpty) {
      val indexEnable = if (getBenchmarkConf(BenchmarkConfig.INDEX_ENABLE).toBoolean) {
        "W/ Index"
      } else {
        "W/O Index"
      }

      s"${getBenchmarkConf(BenchmarkConfig.FILE_FORMAT)} $indexEnable"
    } else {
      // oap !eis & statistics
      getBenchmarkConf(BenchmarkConfig.FILE_FORMAT) + " "
      sqlConf.toArray.map{ setting =>
        val flag = if (setting._2 == "true") {
          ""
        } else {
          "!"
        }
        flag + setting._1.split('.')(4)
      }.mkString(getBenchmarkConf(BenchmarkConfig.FILE_FORMAT) + " ", " & ", "")
    }
  }

  override def toString: String = {
    confName match {
      case Some(name) => name
      case None => configString
    }
  }
}

object BenchmarkConfig {
  val INDEX_ENABLE = "oap.benchmark.config.index"
  val FILE_FORMAT  = "oap.benchmark.config.format"
}

abstract class BenchmarkConfigSelector {
  // TODO: choose conf
  def allConfigurations: Seq[BenchmarkConfig]
}

object BenchmarkConfigSelector {
  // TODO: build config accordingly.
  val wildcardConfiguration: mutable.HashMap[String, String] = mutable.HashMap.empty

  def build(options: Map[String, String]): Unit = {
    wildcardConfiguration ++= options
  }

  def isSelected(config: BenchmarkConfig): Boolean = {
    if (wildcardConfiguration.nonEmpty) {
      wildcardConfiguration.exists{conf =>
        config.getConf(conf._1) == conf._2 ||
        config.confName.equals(conf._2)
      }
    } else {
      true
    }
  }
}

trait ParquetOnlyConfigSet extends BenchmarkConfigSelector{
  // TODO: choose conf
  def allConfigurations: Seq[BenchmarkConfig] = Seq(
    new BenchmarkConfig()
      .setBenchmarkConfName("parquet w/ index")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "parquet")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true"),
    new BenchmarkConfig()
      .setBenchmarkConfName("parquet w/o index")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "parquet")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "false")
  )
}

trait ParquetVsOapConfigSet extends BenchmarkConfigSelector{
  // TODO: choose conf
  def allConfigurations: Seq[BenchmarkConfig] = Seq(
    new BenchmarkConfig()
      .setBenchmarkConfName("oap w/ index")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "oap")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true"),
    new BenchmarkConfig()
      .setBenchmarkConfName("oap w/o index")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "oap")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "false"),
    new BenchmarkConfig()
      .setBenchmarkConfName("parquet w/ index")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "parquet")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true"),
    new BenchmarkConfig()
      .setBenchmarkConfName("parquet w/o index")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "parquet")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "false")
  )
}

trait OapStrategyConfigSet extends BenchmarkConfigSelector{
  // TODO: choose conf
  def allConfigurations: Seq[BenchmarkConfig] = Seq(
    new BenchmarkConfig()
      .setBenchmarkConfName("oapStrategy enabled")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "oap")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true")
      .setSqlConf("spark.sql.oap.oindex.eis.enabled", "false")
      .setSqlConf("spark.sql.oap.strategies.enabled", "true"),
    new BenchmarkConfig()
      .setBenchmarkConfName("oapStrategy disabled")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "oap")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true")
      .setSqlConf("spark.sql.oap.oindex.eis.enabled", "true")
      .setSqlConf("spark.sql.oap.strategies.enabled", "false")
  )
}

trait LocalClusterConfigSet extends BenchmarkConfigSelector {
  // TODO: choose conf
  def allConfigurations: Seq[BenchmarkConfig] = Seq(
    new BenchmarkConfig()
      .setBenchmarkConfName("local cluster 100m offheap")
      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "oap")
      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true")
      .setSparkConf("spark.memory.offHeap.enabled", "true")
      .setSparkConf("spark.memory.offHeap.size", "100m")
    // TODO: Here this config does not work because in local
    // mode, MemoryManager initialization do only once as it
    // is a object. 
//    new BenchmarkConfig()
//      .setBenchmarkConfName("executor on/off heap: 100/0")
//      .setBenchmarkConf(BenchmarkConfig.FILE_FORMAT, "oap")
//      .setBenchmarkConf(BenchmarkConfig.INDEX_ENABLE, "true")
//      .setSparkConf("spark.memory.offHeap.enabled", "true")
//      .setSparkConf("spark.yarn.executor.memoryOverhead", "1g")
//      .setSparkConf("spark.executor.memory", "100g")
//      .setSparkConf("spark.sql.oap.offheap.enable", "false")
  )
}
