export function LoadingTable() {
  return (
    <div className="table-loading" aria-label="Loading frames" role="status">
      {Array.from({ length: 8 }, (_, index) => <span key={index} />)}
    </div>
  )
}
