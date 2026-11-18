import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Article, ArticleRequest, NomDescription, TagSeo } from '../../core/models/models';
import { ReferenceDataService } from '../../core/services/reference-data.service';

@Component({
  selector: 'app-dashboard-article-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './dashboard-article-form.component.html'
})
export class DashboardArticleFormComponent implements OnInit, OnChanges {
  @Input() article: Article | null = null;
  @Output() save = new EventEmitter<{ data: ArticleRequest; image: File | null }>();
  @Output() cancel = new EventEmitter<void>();

  categories: NomDescription[] = [];
  typesArticles: NomDescription[] = [];
  formats: NomDescription[] = [];
  statuts: NomDescription[] = [];
  tagsSeo: TagSeo[] = [];
  imageFile: File | null = null;

  private fb = inject(FormBuilder);

  form = this.fb.group({
    titre: ['', Validators.required],
    resume: [''],
    contenu: [''],
    categorieId: [null as number | null],
    typeArticleId: [null as number | null],
    formatId: [null as number | null],
    statutId: [null as number | null],
    metaTitre: [''],
    metaDescription: [''],
    metaKeywords: [''],
    estEnVedette: [false],
    tagSeoIds: [[] as number[]]
  });

  constructor(private referenceDataService: ReferenceDataService) {}

  ngOnInit(): void {
    this.referenceDataService.getCategories().subscribe((c) => (this.categories = c));
    this.referenceDataService.getTypesArticles().subscribe((t) => (this.typesArticles = t));
    this.referenceDataService.getFormats().subscribe((f) => (this.formats = f));
    this.referenceDataService.getStatuts().subscribe((s) => (this.statuts = s));
    this.referenceDataService.getTagsSeo().subscribe((t) => (this.tagsSeo = t));
    this.patchFromArticle();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['article']) {
      this.patchFromArticle();
    }
  }

  private patchFromArticle(): void {
    if (!this.article) {
      this.form.reset({ estEnVedette: false, tagSeoIds: [] });
      return;
    }
    this.form.patchValue({
      titre: this.article.titre,
      resume: this.article.resume,
      contenu: this.article.contenu,
      categorieId: this.article.categorie?.id ?? null,
      typeArticleId: this.article.typeArticle?.id ?? null,
      formatId: this.article.format?.id ?? null,
      statutId: this.article.statut?.id ?? null,
      metaTitre: this.article.metaTitre,
      metaDescription: this.article.metaDescription,
      metaKeywords: this.article.metaKeywords,
      estEnVedette: this.article.estEnVedette,
      tagSeoIds: this.article.tagsSeo.map((t) => t.id)
    });
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.imageFile = input.files?.[0] ?? null;
  }

  toggleTag(tagId: number): void {
    const current = this.form.value.tagSeoIds ?? [];
    const next = current.includes(tagId) ? current.filter((id) => id !== tagId) : [...current, tagId];
    this.form.patchValue({ tagSeoIds: next });
  }

  isTagSelected(tagId: number): boolean {
    return (this.form.value.tagSeoIds ?? []).includes(tagId);
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.save.emit({ data: this.form.getRawValue() as ArticleRequest, image: this.imageFile });
  }
}
