package com.airbnb.scheduler.api

import com.airbnb.scheduler.jobs.{BaseJob, DependencyBasedJob, ScheduleBasedJob}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{JsonNode, DeserializationContext, JsonDeserializer}
import org.joda.time.Period
import scala.collection.JavaConversions._

/**
 * Custom JSON deserializer for jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
class JobsDeserializer extends JsonDeserializer[BaseJob] {

  def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): BaseJob = {
    val codec = jsonParser.getCodec

    val node = codec.readTree[JsonNode](jsonParser)

    val name = node.get("name").asText
    val command = node.get("command").asText
    val epsilon = {
      if (node.has("epsilon")) Period.parse(node.get("epsilon").asText) else Period.seconds(60)
    }
    val executor =
      if (node.has("executor") && node.get("executor") != null) node.get("executor").asText
      else ""


    val executorFlags =
      if (node.has("executorFlags") && node.get("executorFlags") != null) node.get("executorFlags").asText
      else ""

    val retries =
      if (node.has("retries") && node.get("retries") != null) node.get("retries").asInt
      else 2

    // Init as a list buffer here, conversion to set occurs down below during instantation of Job object.
    val owners = scala.collection.mutable.ListBuffer[String]()
    if (node.has("owners") && node.get("owners") != null) {
      for (owner <- node.path("owners")) {
        owners += owner.asText
      }
    }

    val async =
      if (node.has("async") && node.get("async") != null) node.get("async").asBoolean
      else false

    val disabled =
      if (node.has("disabled") && node.get("disabled") != null) node.get("disabled").asBoolean
      else false
    
    val successCount =
      if (node.has("successCount") && node.get("successCount") != null) node.get("successCount").asLong
      else 0L

    val errorCount =
      if (node.has("errorCount") && node.get("errorCount") != null) node.get("errorCount").asLong
      else 0L

    val lastSuccess =
      if (node.has("lastSuccess") && node.get("lastSuccess") != null) node.get("lastSuccess").asText
      else ""

    val lastError =
      if (node.has("lastError") && node.get("lastError") != null) node.get("lastError").asText
      else ""

    val cpus =
      if (node.has("cpus") && node.get("cpus") != null) node.get("cpus").asDouble
      else 0

    val disks =
      if (node.has("disk") && node.get("disk") != null) node.get("disk").asInt
      else 0

    val mem =
      if (node.has("mem") && node.get("mem") != null) node.get("mem").asInt
      else 0

    var parentList = scala.collection.mutable.ListBuffer[String]()
    if (node.has("parents")) {
      for (parent <- node.path("parents")) {
        parentList += parent.asText
      }
      new DependencyBasedJob(parents = parentList.toSet,
        name = name, command = command, epsilon = epsilon, successCount = successCount, errorCount = errorCount,
        executor = executor, executorFlags = executorFlags, retries = retries, owners = owners.toSet, lastError = lastError,
        lastSuccess = lastSuccess, async = async, cpus = cpus, disk = disks, mem = mem, disabled = disabled)
    } else if (node.has("schedule")) {
      new ScheduleBasedJob(node.get("schedule").asText, name = name, command = command,
        epsilon = epsilon, successCount = successCount, errorCount = errorCount, executor = executor,
        executorFlags = executorFlags, retries = retries, owners = owners.toSet, lastError = lastError,
        lastSuccess = lastSuccess, async = async, cpus = cpus, disk = disks, mem = mem, disabled = disabled)
    } else {
      throw new IllegalStateException("The job found was neither schedule based nor dependency based.")
    }
  }
}
