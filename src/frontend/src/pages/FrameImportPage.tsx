import { AlertCircle, ArrowLeft, CheckCircle2, FileSpreadsheet, LoaderCircle, Upload } from 'lucide-react'
import { type ChangeEvent, type FormEvent, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { importFrames } from '../api/frames'
import { ApiError } from '../api/http'
import type { FrameImportResult } from '../types/frame'
import { inventoryReturnPath } from '../utils/inventoryNavigation'

function fileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function FrameImportPage() {
  const location = useLocation()
  const returnTo = inventoryReturnPath(location.state)
  const [file, setFile] = useState<File>()
  const [uploading, setUploading] = useState(false)
  const [notice, setNotice] = useState<string>()
  const [result, setResult] = useState<FrameImportResult>()

  function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0]
    setResult(undefined)
    setNotice(undefined)
    if (!selected) {
      setFile(undefined)
      return
    }
    if (!selected.name.toLowerCase().endsWith('.csv')) {
      setFile(undefined)
      setNotice('Choose a file with a .csv extension.')
      event.target.value = ''
      return
    }
    setFile(selected)
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) {
      setNotice('Choose a CSV file before starting the import.')
      return
    }
    setUploading(true)
    setNotice(undefined)
    setResult(undefined)
    try {
      setResult(await importFrames(file))
    } catch (error) {
      setNotice(error instanceof ApiError ? error.message : 'An unexpected error occurred while importing the file.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="import-page">
      <Link className="back-link" to={returnTo}><ArrowLeft /> Back to frames</Link>
      <section className="form-heading"><div><h1>Import frames</h1><p>Upload frame inventory from CSV.</p></div></section>

      <div className="import-content">
        <form className="import-tool" onSubmit={submit}>
          <header><FileSpreadsheet /><div><h2>CSV file</h2><p>Up to 5,000 data rows per import.</p></div></header>
          <label className={`file-picker ${uploading ? 'disabled' : ''}`} htmlFor="csvFile">
            <Upload />
            <span>{file ? file.name : 'Choose CSV file'}</span>
            <small>{file ? fileSize(file.size) : 'CSV files only'}</small>
            <input className="sr-only" id="csvFile" aria-label="CSV file" type="file" accept=".csv,text/csv" disabled={uploading} onChange={chooseFile} />
          </label>
          {notice && <div className="import-notice" role="alert"><AlertCircle /><span>{notice}</span></div>}
          <footer><button className="button primary" type="submit" disabled={!file || uploading}>{uploading ? <LoaderCircle className="spin" /> : <Upload />}{uploading ? 'Importing…' : 'Import frames'}</button></footer>
        </form>

        {result && <ImportResult result={result} />}
      </div>
    </div>
  )
}

function ImportResult({ result }: { result: FrameImportResult }) {
  const hasErrors = result.failed > 0
  return (
    <section className={`import-result ${hasErrors ? 'partial' : 'success'}`} aria-live="polite">
      <header><CheckCircle2 /><div><h2>{hasErrors ? 'Import completed with row errors' : 'Import completed'}</h2><p>{hasErrors ? 'Valid rows were imported successfully.' : 'All rows were processed successfully.'}</p></div></header>
      <dl className="import-counts">
        <div><dt>Created</dt><dd>{result.created}</dd></div>
        <div><dt>Updated</dt><dd>{result.updated}</dd></div>
        <div><dt>Unchanged</dt><dd>{result.unchanged}</dd></div>
        <div><dt>Failed</dt><dd>{result.failed}</dd></div>
      </dl>
      {result.errors.length > 0 && <div className="import-errors">
        <h3>Row errors</h3>
        <div className="table-scroll"><table><thead><tr><th>Row</th><th>Frame ID</th><th>Reason</th></tr></thead><tbody>{result.errors.map((error) => <tr key={`${error.rowNumber}-${error.frameId ?? ''}`}><td>{error.rowNumber}</td><td>{error.frameId ?? 'Not provided'}</td><td>{error.message}</td></tr>)}</tbody></table></div>
        {result.errorsTruncated && <p className="truncated-errors"><AlertCircle />Only the first 100 row errors are shown.</p>}
      </div>}
    </section>
  )
}
