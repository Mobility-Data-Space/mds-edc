UPDATE edc_asset
SET dataplane_metadata =
        jsonb_build_object(
                'labels',     '[]'::jsonb,
                'properties', COALESCE(data_address::jsonb, '{}'::jsonb),
                'profiles',   '[]'::jsonb
        )
WHERE dataplane_metadata IS NULL
  AND data_address IS NOT NULL
  AND data_address::jsonb <> '{}'::jsonb;