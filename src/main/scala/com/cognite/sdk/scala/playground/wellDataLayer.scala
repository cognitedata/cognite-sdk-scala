package com.cognite.sdk.scala.playground

final case class Source(
    name: String,
    description: Option[String] = None
)

final case class SourceItems(
    items: Seq[Source]
)

final case class LimitAndCursor(cursor: Option[String] = None, limit: Option[Int] = None)

/// Dummy class that everything can deserialize to if we don't care about the
/// output, or if we want serialize `{}`.
final case class EmptyObj()

final case class WellboreMergeRules(
    name: Seq[String],
    description: Seq[String],
    datum: Seq[String],
    parents: Seq[String],
    wellTops: Seq[String],
    holeSections: Seq[String],
    trajectories: Seq[String],
    casings: Seq[String],
    totalDrillingDays: Seq[String],
    kickoffMeasuredDepth: Seq[String]
)

final case class WellFilterRequest(
    filter: WellFilter,
    cursor: Option[String] = None,
    limit: Option[Int] = None
)

final case class WellFilter(
    source: Option[Seq[String]] = None
)

object WellboreMergeRules {
  def apply(source: Seq[String]): WellboreMergeRules =
    new WellboreMergeRules(
      source,
      source,
      source,
      source,
      source,
      source,
      source,
      source,
      source,
      source
    )
}

final case class WellMergeRules(
    name: Seq[String],
    description: Seq[String],
    country: Seq[String],
    quadrant: Seq[String],
    region: Seq[String],
    block: Seq[String],
    field: Seq[String],
    operator: Seq[String],
    spudDate: Seq[String],
    license: Seq[String],
    wellType: Seq[String],
    waterDepth: Seq[String],
    wellhead: Seq[String]
)

object WellMergeRules {
  def apply(sources: Seq[String]): WellMergeRules =
    new WellMergeRules(
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources,
      sources
    )
}

final case class DeleteSources(
    items: Seq[Source]
)

final case class AssetSource(
    assetExternalId: String,
    sourceName: String
)

final case class DeleteWells(items: Seq[AssetSource], recursive: Boolean = false)

final case class Datum(
    value: Double,
    unit: String,
    reference: String
)

final case class Distance(
    value: Double,
    unit: String
)

final case class Well(
    matchingId: String,
    name: String,
    wellhead: Wellhead,
    sources: Seq[AssetSource],
    description: Option[String] = None,
    uniqueWellIdentifier: Option[String] = None,
    country: Option[String] = None,
    quadrant: Option[String] = None,
    region: Option[String] = None,
    block: Option[String] = None,
    field: Option[String] = None,
    operator: Option[String] = None,
    spudDate: Option[String] = None,
    wellType: Option[String] = None,
    license: Option[String] = None,
    waterDepth: Option[Distance] = None,
    wellbores: Option[Seq[Wellbore]] = None
)

final case class Wellbore(
    matchingId: String,
    name: String,
    wellMatchingId: String,
    sources: Seq[AssetSource],
    description: Option[String] = None,
    parentWellboreMatchingId: Option[String] = None,
    uniqueWellboreIdentifier: Option[String] = None,
    datum: Option[Datum] = None,
    totalDrillingDays: Option[Double] = None,
    kickoffMeasuredDepth: Option[Distance] = None
)

final case class WellboreItems(items: Seq[Wellbore])

final case class Wellhead(
    x: Double,
    y: Double,
    crs: String
)

final case class WellIngestion(
    name: String,
    source: AssetSource,
    matchingId: Option[String] = None,
    description: Option[String] = None,
    uniqueWellIdentifier: Option[String] = None,
    country: Option[String] = None,
    quadrant: Option[String] = None,
    region: Option[String] = None,
    spudDate: Option[String] = None,
    block: Option[String] = None,
    field: Option[String] = None,
    operator: Option[String] = None,
    wellType: Option[String] = None,
    license: Option[String] = None,
    waterDepth: Option[Distance] = None,
    wellhead: Option[Wellhead] = None
)

final case class WellboreIngestion(
    name: String,
    wellAssetExternalId: String,
    source: AssetSource,
    matchingId: Option[String] = None,
    description: Option[String] = None,
    parentWellboreAssetExternalId: Option[String] = None,
    uniqueWellboreIdentifier: Option[String] = None,
    datum: Option[Datum] = None,
    totalDrillingDays: Option[Double] = None,
    kickoffMeasuredDepth: Option[Distance] = None
)

final case class WellboreIngestionItems(
    items: Seq[WellboreIngestion]
)

final case class WellIngestionItems(
    items: Seq[WellIngestion]
)

final case class WellItems(
    items: Seq[Well],
    wellsCount: Option[Int] = None,
    wellboresCount: Option[Int] = None,
    nextCursor: Option[String] = None
)
