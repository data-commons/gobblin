/*
* Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use
* this file except in compliance with the License. You may obtain a copy of the
* License at  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied.
*/

package gobblin.data.management.version.finder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Lists;

import gobblin.dataset.Dataset;
import gobblin.dataset.FileSystemDataset;
import gobblin.data.management.version.FileSystemDatasetVersion;
import gobblin.util.PathUtils;


/**
 * Class to find {@link FileSystemDataset} versions in the file system.
 *
 * Concrete subclasses should implement a ({@link org.apache.hadoop.fs.FileSystem}, {@link java.util.Properties})
 * constructor to be instantiated.
 *
 * @param <T> Type of {@link gobblin.data.management.version.FileSystemDatasetVersion} expected from this class.
 */
public abstract class DatasetVersionFinder<T extends FileSystemDatasetVersion> implements VersionFinder<T> {

  protected FileSystem fs;

  public DatasetVersionFinder(FileSystem fs, Properties props) {
    this.fs = fs;
  }

  public DatasetVersionFinder(FileSystem fs) {
    this(fs, new Properties());
  }

  /**
   * Find dataset versions in the input {@link org.apache.hadoop.fs.Path}. Dataset versions are subdirectories of the
   * input {@link org.apache.hadoop.fs.Path} representing a single manageable unit in the dataset.
   * See {@link gobblin.data.management.retention.DatasetCleaner} for more information.
   *
   * @param dataset {@link org.apache.hadoop.fs.Path} to directory containing all versions of a dataset.
   * @return Map of {@link gobblin.data.management.version.DatasetVersion} and {@link org.apache.hadoop.fs.FileStatus}
   *        for each dataset version found.
   * @throws IOException
   */
  @Override
  public Collection<T> findDatasetVersions(Dataset dataset) throws IOException {
    FileSystemDataset fsDataset = (FileSystemDataset) dataset;
    Path versionGlobStatus = new Path(fsDataset.datasetRoot(), globVersionPattern());
    FileStatus[] dataSetVersionPaths = this.fs.globStatus(versionGlobStatus);

    List<T> dataSetVersions = Lists.newArrayList();
    for (FileStatus dataSetVersionPath : dataSetVersionPaths) {
      T datasetVersion =
          getDatasetVersion(PathUtils.relativizePath(dataSetVersionPath.getPath(), fsDataset.datasetRoot()),
              dataSetVersionPath.getPath());
      if (datasetVersion != null) {
        dataSetVersions.add(datasetVersion);
      }
    }

    return dataSetVersions;
  }

  /**
   * Should return class of T.
   */
  @Override
  public abstract Class<? extends FileSystemDatasetVersion> versionClass();

  /**
   * Glob pattern relative to the root of the dataset used to find {@link org.apache.hadoop.fs.FileStatus} for each
   * dataset version.
   * @return glob pattern relative to dataset root.
   */
  public abstract Path globVersionPattern();

  /**
   * Parse {@link gobblin.data.management.version.DatasetVersion} from the path of a dataset version.
   * @param pathRelativeToDatasetRoot {@link org.apache.hadoop.fs.Path} of dataset version relative to dataset root.
   * @param fullPath full {@link org.apache.hadoop.fs.Path} of the dataset version.
   * @return {@link gobblin.data.management.version.DatasetVersion} for that path.
   */
  public abstract T getDatasetVersion(Path pathRelativeToDatasetRoot, Path fullPath);

}
