# Banner pack architecture

All banner recipes are organized into banner packs. Each recipe belongs to
exactly one pack, and packs serve as the primary unit of organization and
distribution of banner recipes. Each banner pack has an unique pack ID.

## Pack files

The game directory contains a `bannerpacks/` directory. Each immediate
subdirectory or `.zip` file with a `bannerpack.mcmeta` is a pack. Directory
packs are writable; ZIP packs are read-only. Both formats are loaded through
the same pack abstraction.

```text
bannerpacks/
  local/
    bannerpack.mcmeta
    banners/<namespace>/<recipe>.json
  categories/
    bannerpack.mcmeta
    banners/<namespace>/<recipe>.json
    categories/<namespace>/<category>.json
    categories/lang/<locale>.json
  other-pack.zip
```

The only required item inside a pack is `bannerpack.mcmeta`. Recipe and
category directories are optional individually; the remote `categories` pack
contains category metadata without any `banners/` directory.

`local` is the special editable pack for user-created recipes. It is created
automatically when missing and cannot be deleted. Other directory packs are
otherwise handled like ZIP packs for loading and recipe lookup.

The root `bannerpack.mcmeta` object has a `pack` section with `pack_format: 1`
and a display `description`. An optional `bannerpack` section can provide
`author` and `url`. A pack's ID is derived from its directory or ZIP filename,
not from metadata.

Each JSON recipe under `banners/<namespace>/` defines a description, optional
author and URL, category ID, base `banner_color`, and ordered pattern `layers`.
Its recipe ID is derived from the path as `<namespace>:<filename>`; the loader
uses that path ID rather than an ID from the JSON. Category files live under
`categories/<namespace>/`; their filename supplies the category ID, while JSON
can specify a description and icon item. Optional
`categories/lang/<locale>.json` files map category IDs to localized names.

## Repository and runtime data

`AvailableBannerPacks` scans the pack directory, instantiates
`DirectoryBannerPack` or `ZipBannerPack`, and keeps all available packs indexed
by ID. It resolves recipes to their owning pack and provides recipe operations.
Directory packs can add, update, remove, move, and receive copied recipes; ZIP
packs reject writes.

`SelectedBannerPacks` is the view over the available packs selected by the
user. It merges selected-pack category definitions and translations, creates
fallback categories for their recipe category IDs, and exposes only their
recipes. Selected pack IDs are stored in
`config/banner-recipes/bannerpacks.json`; a new config selects only `local`,
and `local` cannot be deselected. Downloading or placing another pack in the
directory makes it available, but does not select it.

The JSON format is:

```json
{
  "selectedPacks": ["local", "categories", "numbers"]
}
```

On startup, configured selected IDs that are not available are removed from
the JSON file.

The download registry records the source URL, filename, SHA-256, and download
time for repository-managed packs. A pack can be available without being
selected.

## Download flow

The online repository index describes available packs, including IDs, titles,
descriptions, download URLs, ZIP SHA-256 hashes, and size/publishing metadata.
`BannerPackRepoClient` fetches that index over HTTPS and downloads ZIP files.
`BannerPackDownloadService` checks for existing files, downloads to a temporary
file, verifies SHA-256 and pack contents, then atomically installs the ZIP.

For this project the index URL is
`https://raw.githubusercontent.com/magicus/banner-recipe-database/refs/heads/main/bannerpack-index-v1.json`.
